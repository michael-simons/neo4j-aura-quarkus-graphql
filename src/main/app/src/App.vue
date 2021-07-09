<template>
  <Books/>
</template>

<script>

import Books from './components/Books.vue'

export default {
  name: 'Goodreads',
  components: {
    Books
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: left;
  color: #2c3e50;
  margin-top: 20px;
}
</style>
