import {ref, watch} from "vue";
import axios from "/src/interceptors/axios";

export default function useSheet() {
    const sheet = ref();
    const _currentPage = ref(1);
    const _pageSize = ref(20);
    const _totalPages = ref(1);
    const _totalElements = ref(20);
    const recordId = ref();
    const details = ref({
        ok: undefined,
        warning: undefined,
        total: undefined,
        amount: undefined
    })

     // Watcher for currentPage
     watch(_currentPage, async (value) => {
        if (value > _totalPages.value) {
            _currentPage.value = _totalPages.value;
        }
        if (value) {
            await fetchSheetData();
        }
    });

    // Watcher for pageSize
    watch(_pageSize, async () => {
        _currentPage.value = 1;
        await fetchSheetData();
    });


    const getInitialFilter = () => ({
        startDate: undefined,
        endDate: undefined,
        totalAmountStart: undefined,
        totalAmountEnd: undefined,
        status: undefined,
    });

    const filter = ref(getInitialFilter())

    const clearFilter = () => {
        Object.assign(filter.value, getInitialFilter());
    }

    const fetchSheetData = async () => {
        sheet.value = undefined
        const params = {
            page: _currentPage.value,
            size: _pageSize.value,
            fileId: recordId.value,
        };

        Object.entries(filter.value)
            .filter(([_, value]) => value)
            .reduce((_, [key, value]) => {
                params[key] = value;
            }, {});

        try {
            const response = await axios.get('excels/filter', {params: params});
            sheet.value = response.data.excPage.content;
            _totalPages.value = response.data.excPage.page.totalPages;
            _totalElements.value = response.data.excPage.page.totalElements;
            details.value.ok = response.data.ok;
            details.value.warning = response.data.warn;
            details.value.total = response.data.excPage.page.totalElements;
            details.value.amount = response.data.totalAmountSum;
        } catch (error) {
            console.error("Error fetching sheet data:", error);
        }
    };

    return {
        sheet,
        _currentPage,
        _totalPages,
        _totalElements,
        _pageSize,
        recordId,
        details,
        fetchSheetData,
        filter,
        clearFilter
    };
}
