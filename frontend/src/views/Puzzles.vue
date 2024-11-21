<!-- Головний компонент для відображення списку задач -->
<template>
    <h1>Задачі різної складності</h1>
    <hr>
    <!-- Панель керування з пагінацією та сортуванням -->
    <div class="pannel">
        <!-- Блок пагінації -->
        <div class="pagination">
            <button 
                class="pagination-btn" 
                :disabled="page <= 1" 
                @click="$router.push(`/puzzles/${(page-=1)}`)"
            >
                <i class="fa fa-chevron-left"></i>
            </button>
            <span class="pagination-info">Сторінка {{ page }}</span>
            <button 
                class="pagination-btn" 
                :disabled="!hasNext" 
                @click="$router.push(`/puzzles/${(page=Number(page) + 1)}`)"
            >
                <i class="fa fa-chevron-right"></i>
            </button>
            <!-- Вибір кількості елементів на сторінці -->
            <div class="items-per-page">
                <label for="pageLimitInput">Кількість на сторінці: </label>
                <input 
                    type="number" 
                    v-model="pageLimit" 
                    min="1" 
                    max="30" 
                    id="pageLimitInput"
                    @change="updatePuzzles"
                >
            </div> 
        </div>
        <!-- Блок сортування -->
        <div class="sorting">
            <h3>Сортувати за: </h3>
            <select v-model="sortBy">
                <option value="id">За замовчуванням</option>
                <option value="title">За назвою</option>
                <option value="description">За описом</option>
                <option value="content">За текстом</option>
                <option value="score">За балами</option>
                <option value="timeLimit">За часом</option>
            </select>
            <select v-model="sortOrder">
                <option value="desc">За спаданням</option>
                <option value="asc">За зростанням</option>
            </select>
        </div>
    </div>
    
    <hr>
    <!-- Список задач -->
    <div class="puzzles">
        <div class="puzzle" v-for="puzzle in puzzles" :key="puzzle.id">
            <div class="puzzle-header">
                <h2>{{ puzzle.title }}</h2>
                <div class="puzzle-header-right">
                    <p class="puzzle-header-right-time">Ліміт часу: {{ puzzle.timeLimit }} с</p>
                    <p class="puzzle-header-right-points">{{ puzzle.score }} балів</p>
                </div>
            </div>
            <div class="puzzle-body">
                <p>{{ puzzle.description }}</p>
            </div>
            <div class="puzzle-footer">
                <LinkBtn img="wave2.svg" click="load" :to="`/puzzle/${puzzle.id}`"><i class="fa fa-eye"></i> Переглянути</LinkBtn>
            </div>
        </div>
    </div>
</template>

<script scoped>
import apiClient from '../axios';
import LinkBtn from '../components/LinkBtn.vue';

export default {
    name: 'Puzzles',
    components: {
        LinkBtn,
    },
    // Початковий стан компонента
    data() {
        return {
            puzzles: [], // Масив задач
            page: 1, // Поточна сторінка
            sortBy: 'id', // Поле для сортування
            sortOrder: 'desc', // Порядок сортування
            hasNext: false, // Чи є наступна сторінка
            pageLimit: 10, // Кількість елементів на сторінці
        }
    },
    // Відслідковування змін для оновлення списку
    watch: {
        sortBy() {
            this.updatePuzzles();
        },
        sortOrder() {
            this.updatePuzzles();
        },
        '$route.params.page': function() {
            this.updatePuzzles();
        }
    },
    methods: {
        // Метод оновлення списку задач
        async updatePuzzles() {
            // Перевірка коректності значення pageLimit
            if (Number(this.pageLimit) == NaN || this.pageLimit < 1) {
                this.pageLimit = 10;
            }
            
            const self = this;

            // Запит до API для отримання задач
            await apiClient.get('/puzzles/all', {
                params: {
                    page: this.page,
                    limit: this.pageLimit,
                    sortBy: this.sortBy,
                    sortOrder: this.sortOrder,
                }
            })
                .then((response) => {
                    this.puzzles = response.data['puzzles'];
                    this.hasNext = response.data['hasNext'];
                })
                .catch((error) => {
                    console.error("Error in request: ", error);
                });
        },
    },
    // Ініціалізація компонента при монтуванні
    mounted() {
        this.page = this.$route.params.page || 1;
        this.updatePuzzles();
    },
}
</script>

<style lang="scss" scoped>
@import '@/assets/scss/variables.scss';
@import '@/assets/scss/mixins.scss';

// Стилі для панелі керування
.pannel {
    @include display-flex($justify-content: space-between);
    gap: 10px;
    box-shadow: var(--weak-box-shadow);
    padding: 10px;
    background-color: var(--weak-color);

    // Стилі для блоку пагінації
    .pagination {
        @include display-flex($align-items: center);
        gap: 10px;

        // Стилі для кнопок пагінації
        button.pagination-btn {
            @include display-flex($justify-content: center);
            display: inline-flex;
            width: 30px;
            height: 30px;
            border-radius: 3px;
            border: none;
            background-color: var(--weak-color);
            box-shadow: var(--weak-box-shadow);
            cursor: pointer;
            transition: background-color 0.2s ease-in-out;

            &:not(:disabled):hover {
                background-color: var(--weak-color-hover);
            }

            &:disabled {
                cursor: not-allowed;
            }
        }

        // Стилі для блоку вибору кількості елементів
        .items-per-page {
            @include display-flex($align-items: center);
            gap: 10px;

            // Стилі для поля введення
            input {
                width: 50px;
                height: 30px;
                border-radius: 3px;
                border: 1px solid var(--secondary-color);
                background-color: var(--weak-color);
                padding: 5px;
                transition: background-color 0.2s ease-in-out;

                // Видалення кнопок спінера для Chrome, Safari, Edge, Opera
                &::-webkit-outer-spin-button,
                &::-webkit-inner-spin-button {
                    -webkit-appearance: none;
                    margin: 0;
                }
                
                // Видалення кнопок спінера для Firefox
                &[type="number"] {
                    -moz-appearance: textfield;
                    appearance: textfield;
                }
            }
        }
        
        // Відступи між елементами пагінації
        *:not(:last-child) {
            margin-right: 10px;
        }
    }

    // Стилі для селектів сортування
    select {
        background-color: var(--weak-color);
        border: none;
        border-radius: 3px;
        box-shadow: var(--weak-box-shadow);
        padding: 5px;
        cursor: pointer;
        transition: background-color 0.2s ease-in-out;
        
        &:not(:last-child) {
            margin-right: 10px;
        }
    }
}

// Стилі для списку задач
.puzzles {
    @include display-flex($flex-direction: column);
    gap: 20px;
    
    // Стилі для окремої задачі
    .puzzle {
        width: 100%;
        padding: 15px;
        background-color: var(--weak-color);
        border-radius: 3px;
        box-shadow: var(--weak-box-shadow);

        // :deep() зберігає стилі для елементів, що не представлені в цьому файлі напряму
        :deep(a) {
            margin-top: 10px;
        }
    }
}
</style>