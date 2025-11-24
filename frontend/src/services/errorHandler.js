import { showErrorModal } from './modalService';
import { useStore } from 'vuex';

/**
 * Handles API errors from the server.
 * @param {Error} error The error object from an Axios catch block.
 * @param {object} [formErrorsRef=null] A Vue reactive reference to store validation errors.
 */
export function handleApiError(error, formErrorsRef = null) {
  const store = useStore();

  if (!error.response || !error.response.data) {
    const genericMessage = 'Невідома помилка мережі або сервера. Спробуйте ще раз пізніше.';
    showErrorModal('Помилка', genericMessage);
    console.error("Unknown API error: ", error);
    return;
  }

  const { data: errorData } = error.response;
  const { code, error: errorMessage, details } = errorData;

  // Handle validation errors
  if (code === 'VALIDATION_ERROR' && details?.fields && formErrorsRef) {
    // Clear previous errors
    Object.keys(formErrorsRef).forEach(key => {
      formErrorsRef[key] = '';
    });
    // Set new errors
    Object.entries(details.fields).forEach(([field, message]) => {
      if (formErrorsRef.hasOwnProperty(field)) {
        formErrorsRef[field] = message;
      }
    });
    store.dispatch('addErrorMessage', 'Будь ласка, виправте помилки у формі.');
    return;
  }
  
  // Handle other known error codes
  let title = 'Помилка';
  let message = errorMessage || 'Сталася невідома помилка.';

  switch (code) {
    case 'UNAUTHORIZED':
      title = 'Помилка авторизації';
      message = 'Ви не авторизовані або ваша сесія закінчилася. Будь ласка, увійдіть знову.';
      break;
    case 'FORBIDDEN':
      title = 'Доступ заборонено';
      message = errorMessage || 'У вас недостатньо прав для виконання цієї дії.';
      break;
    case 'NOT_FOUND':
        title = 'Не знайдено';
        message = errorMessage || 'Запитаний ресурс не знайдено.';
        break;
     case 'BAD_REQUEST':
        title = 'Некоректний запит';
        message = errorMessage || 'Сервер не зміг обробити ваш запит через помилку в даних.';
        break;
  }

  showErrorModal(title, message);
  console.error(`API Error (${code}): `, errorData);
}
