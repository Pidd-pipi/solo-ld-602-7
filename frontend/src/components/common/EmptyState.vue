<script setup lang="ts">
defineProps<{ description?: string; imageSize?: number }>();
</script>

<template>
  <el-empty :description="description ?? '暂无数据'" :image-size="imageSize ?? 80" />
</template>
