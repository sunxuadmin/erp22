<template>
  <div ref="chartEl" class="universal-dashboard-chart" role="img" :aria-label="title" />
</template>

<script setup lang="ts">
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { init, use, type EChartsType } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsOption } from 'echarts';
import type { DashboardBlockType, DashboardSeriesItem, UniversalDashboardTheme } from '../universalDashboard';

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

const props = defineProps<{
  type: Extract<DashboardBlockType, 'donut' | 'bar' | 'line'>;
  title: string;
  data: DashboardSeriesItem[];
  theme: UniversalDashboardTheme;
  showLegend?: boolean;
  showLabels?: boolean;
  limit?: number;
}>();

const chartEl = ref<HTMLDivElement>();
let chart: EChartsType | undefined;
let resizeObserver: ResizeObserver | undefined;

const limitedData = computed(() => (props.data || []).slice(0, Math.max(3, Math.min(20, Number(props.limit || 8)))));

const baseTextStyle = computed(() => ({ color: props.theme.mutedColor, fontFamily: 'inherit' }));

const donutOption = (): EChartsOption => ({
  color: props.theme.chartColors,
  animationDuration: 500,
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c}（{d}%）' },
  legend: props.showLegend === false ? undefined : { bottom: 0, left: 'center', type: 'scroll', textStyle: baseTextStyle.value },
  series: [
    {
      name: props.title,
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', props.showLegend === false ? '50%' : '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: props.theme.backgroundColor, borderWidth: 3, borderRadius: 5 },
      label: { show: props.showLabels !== false, color: props.theme.textColor, formatter: '{b}\n{c}' },
      labelLine: { show: props.showLabels !== false, length: 10, length2: 8 },
      data: limitedData.value.map((item) => ({ name: item.name, value: item.value }))
    }
  ]
});

const barOption = (): EChartsOption => {
  const rows = [...limitedData.value].sort((a, b) => Number(a.value || 0) - Number(b.value || 0));
  return {
    color: props.theme.chartColors,
    animationDuration: 500,
    grid: {
      top: 8,
      right: props.showLabels === false ? 18 : 48,
      bottom: 16,
      left: 18,
      outerBoundsMode: 'same',
      outerBoundsContain: 'axisLabel'
    },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: baseTextStyle.value,
      splitLine: { lineStyle: { color: props.theme.borderColor, type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: rows.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { ...baseTextStyle.value, width: 112, overflow: 'truncate' }
    },
    series: [
      {
        type: 'bar',
        data: rows.map((item, index) => ({
          value: item.value,
          itemStyle: { color: props.theme.chartColors[index % props.theme.chartColors.length], borderRadius: [0, 5, 5, 0] }
        })),
        barMaxWidth: 22,
        label: { show: props.showLabels !== false, position: 'right', color: props.theme.textColor }
      }
    ]
  };
};

const lineOption = (): EChartsOption => ({
  color: props.theme.chartColors,
  animationDuration: 500,
  grid: { top: 18, right: 24, bottom: 18, left: 18, outerBoundsMode: 'same', outerBoundsContain: 'axisLabel' },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: limitedData.value.map((item) => item.name),
    axisLine: { lineStyle: { color: props.theme.borderColor } },
    axisTick: { show: false },
    axisLabel: { ...baseTextStyle.value, width: 90, overflow: 'truncate' }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: baseTextStyle.value,
    splitLine: { lineStyle: { color: props.theme.borderColor, type: 'dashed' } }
  },
  series: [
    {
      name: props.title,
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: limitedData.value.map((item) => item.value),
      lineStyle: { width: 3, color: props.theme.accentColor },
      itemStyle: { color: props.theme.accentColor, borderColor: props.theme.backgroundColor, borderWidth: 2 },
      areaStyle: { color: props.theme.accentColor, opacity: 0.08 },
      label: { show: props.showLabels !== false, position: 'top', color: props.theme.textColor }
    }
  ]
});

const render = () => {
  if (!chartEl.value) return;
  chart ||= init(chartEl.value, undefined, { renderer: 'canvas' });
  const option = props.type === 'donut' ? donutOption() : props.type === 'bar' ? barOption() : lineOption();
  chart.setOption(option, true);
};

watch(
  () => [props.type, props.data, props.theme, props.showLegend, props.showLabels, props.limit],
  () => nextTick(render),
  { deep: true, immediate: true }
);

onMounted(() => {
  render();
  if (chartEl.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => chart?.resize());
    resizeObserver.observe(chartEl.value);
  }
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chart?.dispose();
  chart = undefined;
});
</script>

<style scoped>
.universal-dashboard-chart {
  width: 100%;
  height: 100%;
  min-height: 180px;
}
</style>
