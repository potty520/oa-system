<template>
  <div :class="{ hidden: hidden }" class="pagination-container">
    <el-pagination
      background
      :current-page.sync="currentPage"
      :page-size.sync="pageSize"
      :layout="layout"
      :page-sizes="pageSizes"
      :total="total"
      v-bind="$attrs"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script>
export default {
  name: 'Pagination',
  props: {
    total: {
      required: true,
      type: Number
    },
    page: {
      type: Number,
      default: 1
    },
    limit: {
      type: Number,
      default: 10
    },
    pageSizes: {
      type: Array,
      default() {
        return [10, 20, 30, 50]
      }
    },
    layout: {
      type: String,
      default: 'total, sizes, prev, pager, next, jumper'
    },
    hidden: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    currentPage: {
      get() {
        return this.page
      },
      set(value) {
        this.$emit('update:page', value)
      }
    },
    pageSize: {
      get() {
        return this.limit
      },
      set(value) {
        this.$emit('update:limit', value)
      }
    }
  },
  methods: {
    handleSizeChange(value) {
      this.$emit('pagination', { page: this.currentPage, limit: value })
    },
    handleCurrentChange(value) {
      this.$emit('pagination', { page: value, limit: this.pageSize })
    }
  }
}
</script>

<style scoped>
.pagination-container {
  padding: 10px 0;
  background: #fff;
  text-align: right;
}

.pagination-container.hidden {
  display: none;
}
</style>