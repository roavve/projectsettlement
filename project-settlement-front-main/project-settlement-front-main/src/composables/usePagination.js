import { computed, ref } from "vue";

export default function usePagination(currentPage, pageSize, totalPages, totalElements) {
    const showOptions = ref(false);

    const startIndex = computed(() => (currentPage.value - 1) * pageSize.value + 1);

    const endIndex = computed(() => {
        const calculatedEndIndex = startIndex.value + pageSize.value - 1;
        return Math.min(calculatedEndIndex, totalElements.value);
    });

    const previousPage = () => {
        if (currentPage.value > 1) {
            currentPage.value--;
        }
    };

    const nextPage = () => {
        if (currentPage.value < totalPages.value) {
            currentPage.value++;
        }
    };

    const onEnter = (event) => {
        const page = parseInt(event.target.value, 10);
        currentPage.value = page >= 1 && page <= totalPages.value ? page : 1;
    };

    const validatePage = (event) => {
        let page = parseInt(event.target.value, 10);
        if (event.target.value) {
            page = Math.max(1, Math.min(page, totalPages.value));
            event.target.value = page;
        }
    };

    const options = computed(() => {
        const predefinedOptions = [10, 20, 50, 100];
        const availableOptions = predefinedOptions.filter(option => option < totalElements.value);
        availableOptions.push(totalElements.value);
        return availableOptions;
    });

    return {
        showOptions,
        startIndex,
        endIndex,
        previousPage,
        nextPage,
        onEnter,
        validatePage,
        options,
    };
}
