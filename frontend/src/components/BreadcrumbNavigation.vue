<template>
  <nav class="breadcrumb-nav">
    <router-link :to="homeRoute.to" class="crumb home">
      <i class="fa-solid fa-arrow-left"></i>
      <span>{{ homeRoute.name }}</span>
    </router-link>
    <template v-if="crumbs.length > 0">
      <span class="separator">/</span>
      <div class="crumbs-trail">
        <template v-for="(crumb, index) in crumbs" :key="crumb.to || crumb.name">
          <router-link v-if="index < crumbs.length - 1 && crumb.to" :to="crumb.to" class="crumb link">
            {{ crumb.name }}
          </router-link>
          <span v-else class="crumb current">
            {{ crumb.name }}
          </span>
          <span v-if="index < crumbs.length - 1" class="separator">/</span>
        </template>
      </div>
    </template>
  </nav>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  // Пример: [{ name: 'Група', to: '/group/1' }, { name: 'Учасники' }]
  crumbs: {
    type: Array,
    default: () => []
  },
  // Опорная точка, куда ведет кнопка "назад"
  homeRoute: {
    type: Object,
    default: () => ({ name: 'На головну', to: '/' })
  }
});
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.breadcrumb-nav {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
  padding: 10px 15px;
  background-color: var(--weak-color);
  border: 1px solid var(--secondary-color);
  border-radius: 5px;
  margin-bottom: 1.5rem;
  font-family: $form-font;
  font-size: 0.9em;
  overflow-x: auto;
}

.crumb {
  color: var(--secondary-color);
  text-decoration: none;
  white-space: nowrap;

  &.home {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: var(--text-color);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  &.link {
    color: var(--secondary-color);
    &:hover {
      color: var(--text-color);
      text-decoration: underline;
    }
  }
  
  &.current {
    font-weight: 600;
    color: var(--text-color);
  }
}

.separator {
  color: var(--secondary-color);
  margin: 0 4px;
}

.crumbs-trail {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
}
</style>
