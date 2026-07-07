import {defineStore} from 'pinia';
import {ref} from 'vue';

export const useFilterStore = defineStore('filter', () => {
    const getInitialFilter = () => ({
        region: 'აირჩიეთ რეგიონი',
        serviceCenter: 'აირჩიეთ მ/ც',
        withdrawType: [],
        status: 'ჩანაწერის სტატუსი',
        orderStatus: 'ორდერის სტატუსი',
        totalAmountStart: undefined,
        totalAmountEnd: undefined,

        orderN: '',
        projectID: '',
        id: '',
        purpose: '',
        tax: '',
        description: '',

        clarificationDateStart: undefined,
        clarificationDateEnd: undefined,

        /*changeDateStart: undefined,
        changeDateEnd: undefined,*/

        transferDateStart: undefined,
        transferDateEnd: undefined,

        extractionDateStart: undefined,
        extractionDateEnd: undefined,

        note: '',
        history: '',
        change_person: undefined,
    });

    let filter = ref(getInitialFilter());

    const clearFilter = () => {
        Object.assign(filter.value, getInitialFilter());
    };

    return {filter, clearFilter};
});

