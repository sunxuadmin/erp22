package org.dromara.crehn.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class MediaMetadataParser {

    public MediaMetadata parse(MultipartFile file, String ext, String mimeType) {
        MediaMetadata metadata = new MediaMetadata();
        String mediaType = detectMediaType(ext, mimeType);
        metadata.setMediaType(mediaType);
        if ("image".equals(mediaType)) {
            parseImage(file, metadata);
        } else if ("video".equals(mediaType)) {
            parseVideo(file, ext, metadata);
        }
        if (StringUtils.isBlank(metadata.getMetadataJson())) {
            metadata.setMetadataJson(JsonUtils.toJsonString(metadata));
        }
        return metadata;
    }

    public MediaMetadata parse(Path filePath, String ext, String mimeType) {
        MediaMetadata metadata = new MediaMetadata();
        String mediaType = detectMediaType(ext, mimeType);
        metadata.setMediaType(mediaType);
        if ("image".equals(mediaType)) {
            parseImage(filePath, metadata);
        } else if ("video".equals(mediaType)) {
            parseVideo(filePath, metadata);
        }
        if (StringUtils.isBlank(metadata.getMetadataJson())) {
            metadata.setMetadataJson(JsonUtils.toJsonString(metadata));
        }
        return metadata;
    }

    private String detectMediaType(String ext, String mimeType) {
        String normalizedExt = StringUtils.blankToDefault(ext, "").toLowerCase(Locale.ROOT);
        String normalizedMime = StringUtils.blankToDefault(mimeType, "").toLowerCase(Locale.ROOT);
        if (normalizedMime.startsWith("image/") || normalizedExt.matches("jpg|jpeg|png|gif|webp|bmp|tif|tiff")) {
            return "image";
        }
        if (normalizedMime.startsWith("video/") || normalizedExt.matches("mp4|mov|mpg|mpeg|avi|mkv|webm")) {
            return "video";
        }
        if (normalizedMime.startsWith("audio/") || normalizedExt.matches("mp3|wav|m4a|aac")) {
            return "audio";
        }
        return "document";
    }

    private void parseImage(MultipartFile file, MediaMetadata metadata) {
        try (InputStream input = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            parseImageInput(imageInput, metadata);
        } catch (Exception ignored) {
            // Missing image metadata should not block upload unless a rule requires it.
        }
    }

    private void parseImage(Path filePath, MediaMetadata metadata) {
        try (InputStream input = Files.newInputStream(filePath); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            parseImageInput(imageInput, metadata);
        } catch (Exception ignored) {
            // Missing image metadata should not block upload unless a rule requires it.
        }
    }

    private void parseImageInput(ImageInputStream imageInput, MediaMetadata metadata) throws IOException {
            if (imageInput == null) {
                return;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                return;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                BufferedImage image = reader.read(0);
                if (image != null) {
                    metadata.setWidth(image.getWidth());
                    metadata.setHeight(image.getHeight());
                }
                metadata.setDpi(readDpi(reader.getImageMetadata(0)));
            } finally {
                reader.dispose();
            }
    }

    private Integer readDpi(IIOMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            Node tree = metadata.getAsTree("javax_imageio_1.0");
            Double horizontalPixelSize = findDoubleAttribute(tree, "HorizontalPixelSize", "value");
            if (horizontalPixelSize != null && horizontalPixelSize > 0) {
                return (int) Math.round(25.4 / horizontalPixelSize);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Double findDoubleAttribute(Node node, String nodeName, String attrName) {
        if (node == null) {
            return null;
        }
        if (nodeName.equals(node.getNodeName())) {
            NamedNodeMap attrs = node.getAttributes();
            Node attr = attrs == null ? null : attrs.getNamedItem(attrName);
            if (attr != null) {
                return parseDouble(attr.getNodeValue());
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Double value = findDoubleAttribute(children.item(i), nodeName, attrName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void parseVideo(MultipartFile file, String ext, MediaMetadata metadata) {
        Path temp = null;
        try {
            temp = Files.createTempFile("crehn-upload-", "." + StringUtils.blankToDefault(ext, "bin"));
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            parseVideo(temp, metadata);
        } catch (Exception ignored) {
            // ffprobe is optional at runtime; upload validation decides whether missing metadata is fatal.
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Best effort temp cleanup.
                }
            }
        }
    }

    private void parseVideo(Path filePath, MediaMetadata metadata) {
        try {
            Process process = new ProcessBuilder("ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", filePath.toString())
                .redirectErrorStream(true)
                .start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return;
            }
            String output = new String(process.getInputStream().readAllBytes());
            int exit = process.exitValue();
            if (exit != 0 || StringUtils.isBlank(output)) {
                return;
            }
            metadata.setMetadataJson(output);
            Map<String, Object> root = JsonUtils.parseObject(output, new TypeReference<Map<String, Object>>() {
            });
            applyVideoMetadata(root, metadata);
        } catch (Exception ignored) {
            // ffprobe is optional at runtime; upload validation decides whether missing metadata is fatal.
        }
    }

    @SuppressWarnings("unchecked")
    private void applyVideoMetadata(Map<String, Object> root, MediaMetadata metadata) {
        if (root == null) {
            return;
        }
        Object streamsObj = root.get("streams");
        if (streamsObj instanceof Iterable<?> streams) {
            for (Object streamObj : streams) {
                if (!(streamObj instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> stream = (Map<String, Object>) raw;
                if (!"video".equals(String.valueOf(stream.get("codec_type")))) {
                    continue;
                }
                metadata.setWidth(parseInteger(stream.get("width")));
                metadata.setHeight(parseInteger(stream.get("height")));
                metadata.setFps(parseFps(stream.get("avg_frame_rate")));
                metadata.setBitrate(parseLong(stream.get("bit_rate")));
                metadata.setDurationSeconds(parseDouble(stream.get("duration")));
                break;
            }
        }
        Object formatObj = root.get("format");
        if (formatObj instanceof Map<?, ?> raw) {
            Map<String, Object> format = (Map<String, Object>) raw;
            if (metadata.getDurationSeconds() == null) {
                metadata.setDurationSeconds(parseDouble(format.get("duration")));
            }
            if (metadata.getBitrate() == null) {
                metadata.setBitrate(parseLong(format.get("bit_rate")));
            }
        }
    }

    private Double parseFps(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.contains("/")) {
            String[] parts = text.split("/");
            Double top = parseDouble(parts[0]);
            Double bottom = parts.length > 1 ? parseDouble(parts[1]) : null;
            if (top != null && bottom != null && bottom != 0) {
                return top / bottom;
            }
        }
        return parseDouble(text);
    }

    private Integer parseInteger(Object value) {
        Double parsed = parseDouble(value);
        return parsed == null ? null : parsed.intValue();
    }

    private Long parseLong(Object value) {
        Double parsed = parseDouble(value);
        return parsed == null ? null : parsed.longValue();
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
