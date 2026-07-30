-- --------------------------------------------------
-- Art Review M87 file review and preview message localization
-- Safe to rerun on MariaDB 10.11+
-- --------------------------------------------------

-- Project validation JSON: historical manual review tips and warnings.
update project
set validation_result_json = replace(validation_result_json,
    'Manual review: video no province/school/name/adviser confirmation was not checked.',
    '人工审核：未勾选视频不含省份、学校、姓名、指导教师等身份信息确认项。')
where validation_result_json like '%Manual review: video no province/school/name/adviser confirmation was not checked.%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify performance video format, resolution, frame rate, bitrate, duration, file size, fixed camera, and synchronous audio/video.',
    '人工审核：请核验表演视频格式、分辨率、帧率、码率、时长、文件大小、固定机位及音画同步情况。')
where validation_result_json like '%Manual review: verify performance video format, resolution, frame rate, bitrate, duration, file size, fixed camera, and synchronous audio/video.%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: performance video must not show province, school, student name, or adviser name.',
    '人工审核：表演视频不得出现省份、学校、学生姓名或指导教师姓名。')
where validation_result_json like '%Manual review: performance video must not show province, school, student name, or adviser name.%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify original or authorization commitment materials.',
    '人工审核：请核验原创或授权承诺材料。')
where validation_result_json like '%Manual review: verify original or authorization commitment materials.%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify artwork image/video format, size, DPI, resolution, bitrate, duration, subtitles, and source attribution according to the activity notice.',
    '人工审核：请按活动通知核验作品图片/视频格式、大小、DPI、分辨率、码率、时长、字幕和来源说明。')
where validation_result_json like '%Manual review: verify artwork image/video format, size, DPI, resolution, bitrate, duration, subtitles, and source attribution according to the activity notice.%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify the paper has not been publicly published.',
    '人工审核：请核验学术论文未公开发表承诺。')
where validation_result_json like '%Manual review: verify the paper has not been publicly published.%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify teaching reform case video format, size, and duration: ',
    '人工审核：请核验教学改革案例视频格式、大小和时长：')
where validation_result_json like '%Manual review: verify teaching reform case video format, size, and duration:%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify teaching reform case image format, size, and DPI: ',
    '人工审核：请核验教学改革案例图片格式、大小和 DPI：')
where validation_result_json like '%Manual review: verify teaching reform case image format, size, and DPI:%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify workshop video format and duration; recommended duration is within ',
    '人工审核：请核验工作坊视频格式和时长，建议时长不超过 ')
where validation_result_json like '%Manual review: verify workshop video format and duration; recommended duration is within %';

update project
set validation_result_json = replace(validation_result_json, ' seconds', ' 秒')
where validation_result_json like '%人工审核：请核验工作坊视频格式和时长，建议时长不超过 % seconds%';

update project
set validation_result_json = replace(validation_result_json,
    'Manual review: verify the workshop has not previously won an award.',
    '人工审核：请核验该工作坊是否未曾获奖。')
where validation_result_json like '%Manual review: verify the workshop has not previously won an award.%';

update project
set validation_result_json = replace(validation_result_json,
    'image DPI missing; manual review required: ',
    '图片 DPI 无法识别，需人工复核：')
where validation_result_json like '%image DPI missing; manual review required:%';

-- Project file preview messages.
update project_file
set preview_message = replace(preview_message,
    'PDF preview queued; please refresh later',
    'PDF 预览任务已排队，请稍后刷新')
where preview_message like '%PDF preview queued; please refresh later%';

update project_file
set preview_message = replace(preview_message,
    'PDF preview queued; retry requested',
    'PDF 预览任务已重新排队，请稍后刷新')
where preview_message like '%PDF preview queued; retry requested%';

update project_file
set preview_message = replace(preview_message,
    'Previous PDF preview conversion was interrupted, requeued',
    '上一次 PDF 预览转换被中断，已重新排队')
where preview_message like '%Previous PDF preview conversion was interrupted, requeued%';

update project_file
set preview_message = replace(preview_message,
    'Generating PDF preview',
    '正在生成 PDF 预览')
where preview_message like '%Generating PDF preview%';

update project_file
set preview_message = replace(preview_message,
    'PDF preview generated',
    'PDF 预览已生成')
where preview_message like '%PDF preview generated%';

update project_file
set preview_message = replace(preview_message,
    'PDF preview conversion failed: ',
    'PDF 预览转换失败：')
where preview_message like '%PDF preview conversion failed:%';

update project_file
set preview_message = replace(preview_message,
    'PDF preview conversion failed',
    'PDF 预览转换失败')
where preview_message like '%PDF preview conversion failed%';

update project_file
set preview_message = replace(preview_message,
    'Office file uploaded, PDF preview conversion failed. Install LibreOffice/soffice or configure crehn.office.soffice-path. Details: ',
    '文件已上传，但 PDF 预览转换失败。请安装 LibreOffice/soffice，或配置 crehn.office.soffice-path。详细信息：')
where preview_message like '%Office file uploaded, PDF preview conversion failed. Install LibreOffice/soffice or configure crehn.office.soffice-path. Details:%';

update project_file
set preview_message = replace(preview_message,
    'Office file uploaded, PDF preview conversion failed. Install LibreOffice/soffice or configure crehn.office.soffice-path.',
    '文件已上传，但 PDF 预览转换失败。请安装 LibreOffice/soffice，或配置 crehn.office.soffice-path。')
where preview_message like '%Office file uploaded, PDF preview conversion failed. Install LibreOffice/soffice or configure crehn.office.soffice-path.%';

update project_file
set preview_message = replace(preview_message,
    'Office file uploaded, PDF preview conversion failed: ',
    '文件已上传，但 PDF 预览转换失败：')
where preview_message like '%Office file uploaded, PDF preview conversion failed:%';

update project_file
set preview_message = replace(preview_message,
    'Office file uploaded, but PDF preview was not generated.',
    '文件已上传，但未生成 PDF 预览文件。')
where preview_message like '%Office file uploaded, but PDF preview was not generated.%';

update project_file
set preview_message = replace(preview_message,
    'Office file uploaded, but generated preview is not a valid PDF.',
    '文件已上传，但生成的预览文件不是有效 PDF。')
where preview_message like '%Office file uploaded, but generated preview is not a valid PDF.%';

update project_file
set preview_message = replace(preview_message,
    'Office file uploaded, PDF preview conversion was interrupted.',
    '文件已上传，但 PDF 预览转换被中断。')
where preview_message like '%Office file uploaded, PDF preview conversion was interrupted.%';

update project_file
set preview_message = replace(preview_message,
    'source file path is empty',
    '源文件路径为空')
where preview_message like '%source file path is empty%';

update project_file
set preview_message = replace(preview_message,
    '; max retry reached',
    '；已达到最大重试次数')
where preview_message like '%; max retry reached%';

-- Project file technical check messages.
update project_file
set check_message = replace(check_message,
    'performance video must be MP4/MOV: ',
    '表演视频必须为 MP4/MOV 格式：')
where check_message like '%performance video must be MP4/MOV:%';

update project_file
set check_message = replace(check_message,
    'performance video must not exceed ',
    '表演视频大小不能超过 ')
where check_message like '%performance video must not exceed %';

update project_file
set check_message = replace(check_message,
    'video resolution is below ',
    '视频分辨率不能低于 ')
where check_message like '%video resolution is below %';

update project_file
set check_message = replace(check_message,
    'video frame rate must be about ',
    '视频帧率需约为 ')
where check_message like '%video frame rate must be about %';

update project_file
set check_message = replace(check_message,
    'video bitrate is below ',
    '视频码率不能低于 ')
where check_message like '%video bitrate is below %';

update project_file
set check_message = replace(check_message,
    'performance video duration exceeds configured limit: ',
    '表演视频时长超过配置限制：')
where check_message like '%performance video duration exceeds configured limit:%';

update project_file
set check_message = replace(check_message,
    'artwork image must be JPG/JPEG: ',
    '作品图片必须为 JPG/JPEG 格式：')
where check_message like '%artwork image must be JPG/JPEG:%';

update project_file
set check_message = replace(check_message,
    'artwork image must be at least ',
    '作品图片大小不能小于 ')
where check_message like '%artwork image must be at least %';

update project_file
set check_message = replace(check_message,
    'image DPI missing; manual review required: ',
    '图片 DPI 无法识别，需人工复核：')
where check_message like '%image DPI missing; manual review required:%';

update project_file
set check_message = replace(check_message,
    'artwork image DPI is below ',
    '作品图片 DPI 不能低于 ')
where check_message like '%artwork image DPI is below %';

update project_file
set check_message = replace(check_message,
    'film artwork video must be MP4/MOV: ',
    '影视类作品视频必须为 MP4/MOV 格式：')
where check_message like '%film artwork video must be MP4/MOV:%';

update project_file
set check_message = replace(check_message,
    'film artwork video must not exceed ',
    '影视类作品视频大小不能超过 ')
where check_message like '%film artwork video must not exceed %';

update project_file
set check_message = replace(check_message,
    'film artwork video duration exceeds configured limit: ',
    '影视类作品视频时长超过配置限制：')
where check_message like '%film artwork video duration exceeds configured limit:%';

update project_file
set check_message = replace(check_message,
    'film artwork video bitrate is below configured limit: ',
    '影视类作品视频码率低于配置要求：')
where check_message like '%film artwork video bitrate is below configured limit:%';

update project_file
set check_message = replace(check_message,
    'teaching reform case video must be MP4/MOV: ',
    '教学改革案例视频必须为 MP4/MOV 格式：')
where check_message like '%teaching reform case video must be MP4/MOV:%';

update project_file
set check_message = replace(check_message,
    'teaching reform case video must not exceed ',
    '教学改革案例视频大小不能超过 ')
where check_message like '%teaching reform case video must not exceed %';

update project_file
set check_message = replace(check_message,
    'teaching reform case video duration exceeds configured limit: ',
    '教学改革案例视频时长超过配置限制：')
where check_message like '%teaching reform case video duration exceeds configured limit:%';

update project_file
set check_message = replace(check_message,
    'teaching reform case image must be JPG/JPEG: ',
    '教学改革案例图片必须为 JPG/JPEG 格式：')
where check_message like '%teaching reform case image must be JPG/JPEG:%';

update project_file
set check_message = replace(check_message,
    'teaching reform case image must be at least ',
    '教学改革案例图片大小不能小于 ')
where check_message like '%teaching reform case image must be at least %';

update project_file
set check_message = replace(check_message,
    'teaching reform case image DPI is below ',
    '教学改革案例图片 DPI 不能低于 ')
where check_message like '%teaching reform case image DPI is below %';

update project_file
set check_message = replace(check_message,
    'workshop video must be MP4/MPG/MPEG: ',
    '工作坊视频必须为 MP4/MPG/MPEG 格式：')
where check_message like '%workshop video must be MP4/MPG/MPEG:%';

update project_file
set check_message = replace(check_message,
    'workshop video duration exceeds configured limit: ',
    '工作坊视频时长超过配置限制：')
where check_message like '%workshop video duration exceeds configured limit:%';
