<template>
    <div class="block" :class="{'opened': opened}">
        <span class="title">Дивиться деталі створення перевіряючої програми</span>
        <i class="arrow fa-solid fa-arrow-right-long" @click="toggle()" :class="{'opened': opened}"></i>
        <div class="content">
            <div class="details">
                <ul>
                    <li>Завжди скидайте буфер виводу <strong>(flush)</strong> - якщо ви цього не зробите, програма користувача зависне у очікуванні вводу.
                        <ul>
                            <li>Наприклад, у C++ це cout.flush(); або cout << flush; або cout << endl;</li>
                            <li>У Python print(..., flush=True) або sys.stdout.flush()</li>
                        </ul>
                    </li>
                    <li>Результат пишіть у:
                        <ul>
                            <li>/code/result/score - Оцінка у відсотках числом з точкою від 0 до 100, наприклад: 98</li>
                            <li>/code/result/message - Сюди ви можете записати повідомлення, яке буде показано користувачу. Це не обов'язково</li>
                        </ul>
                    </li>
                    <li>Лог програми ви можете писати у файл /code/checker.log
                        <ul>
                            <li>ви зможете його дивитись на сторінці окремого checker'а</li>
                            <li>На сервері діють певні обмеження на його розмір, після чого старі логи почнуть видалятися. Тому пишіть туди лише важливі повідомлення. </li>
                            <li><strong>Увага!</strong> Лог треба писати лише за допомогою append у нього повідомлень, ніяк інакше. </li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue';

const opened = ref(false);

const toggle = () => {
    opened.value = !opened.value;
}
</script>

<style lang="scss" scoped>
    @import "@/assets/scss/variables.scss";

    .block {
        width: 100%;
        opacity: 0.85;

        ul {
            margin-left: 35px;
        }

        .arrow {
            cursor: pointer;
            transition: all .2s ease-in-out;
            margin-left: 15px;
            padding: 10px;
            border-radius: 50%;
            border: 1px solid var(--secondary-color);

            &.opened {
                transform: rotate(90deg);
            }
        }

        .content {
            position: relative;
            overflow: hidden;

            .details {
                margin-top: -100%;
                transition: all .5s ease-in-out;
            }
        }

        &.opened {
            .content .details {
                margin-top: 0%;
            }
        }
    }
</style>