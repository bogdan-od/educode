import apiClient from '@/axios.js'

const checkerService = {
    /**
     * Получить список доступных checker'ов пользователя
     * @returns {Promise<Array>} Список checker'ов
     */
    async getCheckers() {
        try {
            const response = await apiClient.get('/checker/my')
            return response.data || []
        } catch (error) {
            console.error('Error fetching checkers:', error)
            throw error
        }
    }
}

export default checkerService