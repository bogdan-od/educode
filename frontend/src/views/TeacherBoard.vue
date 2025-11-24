<template>
    <Preloader v-if="isLoading"/>
    <section v-else class="teacher">
        <LinkBtn to="/add/checker" anim="go" click="load">Створити checker</LinkBtn>
        <LinkBtn to="/checkers" anim="go" click="load">Ваші checker'и</LinkBtn>
    </section>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import Preloader from "@/components/Preloader.vue";
import LinkBtn from "@/components/LinkBtn.vue";
import {showInfoModal} from "@/services/modalService.js";

const isLoading = ref(false);

showInfoModal("Hello", "123");

</script>