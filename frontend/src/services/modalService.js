import store from "@/store";

const showInfoModal = (title, message, confirm = () => {}, cancel = () => {}, close = () => {}) => {
    store.dispatch('showInfoModal', {title: title, message: message, confirm: confirm, cancel: cancel, close: close});
};

const showWarningModal = (title, message, confirm = () => {}, cancel = () => {}, close = () => {}) => {
    store.dispatch('showWarningModal', {title: title, message: message, confirm: confirm, cancel: cancel, close: close});
};

const showErrorModal = (title, message, confirm = () => {}, cancel = () => {}, close = () => {}) => {
    store.dispatch('showErrorModal', {title: title, message: message, confirm: confirm, cancel: cancel, close: close});
};

const showSuccessModal = (title, message, confirm = () => {}, cancel = () => {}, close = () => {}) => {
    store.dispatch('showSuccessModal', {title: title, message: message, confirm: confirm, cancel: cancel, close: close});
};

const showQuestionModal = (title, message, confirm = () => {}, cancel = () => {}, close = () => {}) => {
    store.dispatch('showQuestionModal', {title: title, message: message, confirm: confirm, cancel: cancel, close: close});
};

export { showInfoModal, showWarningModal, showErrorModal, showSuccessModal, showQuestionModal }
