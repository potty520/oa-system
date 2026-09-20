<template>
  <div class="scroll-container" ref="scrollContainer" @wheel.prevent.prevent="handleScroll">
    <div
      ref="scrollWrapper"
      :class="['scroll-wrapper', { 'scroll-moving': moving }]"
      :style="{ left: left + 'px' }"
      @mousedown="startScroll"
    >
      <slot />
    </div>
  </div>
</template>

<script>
const tagAndTagSpacing = 4

export default {
  name: 'ScrollPane',
  data() {
    return {
      left: 0,
      moving: false,
      startX: 0,
      startLeft: 0
    }
  },
  computed: {
    scrollWrapper() {
      return this.$refs.scrollWrapper
    }
  },
  mounted() {
    this.scrollWrapper.addEventListener('scroll', this.emitScroll, true)
  },
  beforeDestroy() {
    this.scrollWrapper.removeEventListener('scroll', this.emitScroll, true)
  },
  methods: {
    handleScroll(e) {
      const eventDelta = e.wheelDelta || -e.deltaY * 40
      const $container = this.$refs.scrollContainer
      const $containerWidth = $container.offsetWidth
      const $wrapper = this.$refs.scrollWrapper
      const $wrapperWidth = $wrapper.offsetWidth

      if (eventDelta > 0) {
        this.left = Math.min(0, this.left + eventDelta)
      } else {
        if ($containerWidth - 100 < $wrapperWidth) {
          this.left = Math.max($containerWidth - $wrapperWidth - 100, this.left + eventDelta)
        } else {
          this.left = 0
        }
      }
    },
    startScroll(e) {
      this.moving = true
      this.startX = e.pageX
      this.startLeft = this.left
      document.onmousemove = (e) => {
        this.left = this.startLeft + (e.pageX - this.startX)
        this.$emit('scroll')
      }
      document.onmouseup = () => {
        this.moving = false
        document.onmousemove = null
        document.onmouseup = null
      }
    },
    toFirst() {
      this.left = 0
    },
    toLast() {
      const $container = this.$refs.scrollContainer
      const $containerWidth = $container.offsetWidth
      const $wrapper = this.$refs.scrollWrapper
      const $wrapperWidth = $wrapper.offsetWidth
      this.left = $containerWidth - $wrapperWidth - 100
    },
    emitScroll() {
      this.$emit('scroll')
    }
  }
}
</script>

<style lang="scss" scoped>
.scroll-container {
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  width: 100%;

  .scroll-wrapper {
    position: absolute;
  }

  .scroll-moving {
    transition: left 0.3s ease;
  }
}
</style>
