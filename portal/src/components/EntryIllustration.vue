<script setup lang="ts">
defineProps<{ kind: string }>();
</script>

<template>
  <span class="entry-illustration" :class="`entry-illustration--${kind}`" aria-hidden="true">
    <svg v-if="kind === 'university'" viewBox="0 0 260 150" role="presentation">
      <path d="M28 62 126 18l104 42-102 48L28 62Z" fill="currentColor" opacity=".38" />
      <path d="m67 79 62 29 66-31v30c-20 25-101 27-128 1V79Z" fill="currentColor" opacity=".22" />
      <path d="m226 61 0 50" stroke="currentColor" stroke-width="8" stroke-linecap="round" opacity=".5" />
      <circle cx="226" cy="119" r="8" fill="currentColor" opacity=".58" />
      <path d="M30 66 128 109 230 62" fill="none" stroke="#fff" stroke-width="3" opacity=".58" />
    </svg>

    <svg v-else-if="kind === 'school'" viewBox="0 0 260 150" role="presentation">
      <path d="M22 42c45-14 84-1 108 24v70c-30-27-65-35-108-22V42Z" fill="currentColor" opacity=".31" />
      <path d="M238 42c-45-14-84-1-108 24v70c30-27 65-35 108-22V42Z" fill="currentColor" opacity=".22" />
      <path d="M130 67v69" stroke="#fff" stroke-width="4" opacity=".68" />
      <path d="M34 55c34-7 63 2 84 21M226 55c-34-7-63 2-84 21" fill="none" stroke="#fff" stroke-width="3" opacity=".46" />
      <path d="M17 118c47-13 84-3 113 18 29-21 66-31 113-18" fill="none" stroke="currentColor" stroke-width="8" stroke-linecap="round" opacity=".18" />
    </svg>

    <svg v-else viewBox="0 0 260 150" role="presentation">
      <g transform="translate(120 -2) rotate(32 62 76)">
        <path d="M44 16h44l18 73-40 48-40-48 18-73Z" fill="currentColor" opacity=".3" />
        <path d="M66 34v65" stroke="#fff" stroke-width="5" opacity=".66" />
        <circle cx="66" cy="90" r="10" fill="none" stroke="#fff" stroke-width="4" opacity=".74" />
        <path d="M39 17h54M35 8h62" stroke="currentColor" stroke-width="8" stroke-linecap="round" opacity=".52" />
      </g>
      <path d="M25 132c57-17 119-14 205 1" fill="none" stroke="currentColor" stroke-width="7" stroke-linecap="round" opacity=".16" />
    </svg>
  </span>
</template>
