import {ref, watch} from "vue";
import axios from "../interceptors/axios";
import {useFilterStore} from "/src/stores/filter.js";
import qs from "qs";

export default function useUploads() {
    const {filter} = useFilterStore();
    const sheets = ref();
    const currentPage = ref(1);
    const pageSize = ref(20);
    const selectedSheet = ref();
    const lastResponse = ref();
    const totalPages = ref();
    const totalElements = ref();
    const records = ref()
    const sortBy = ref()
    const sortDir = ref()

    const fetchSheets = async () => {
        try {
            sheets.value = undefined;
            lastResponse.value = await axios.get(`extraction-task/all-upls`, {
                params: {
                    page: currentPage.value,
                    size: pageSize.value,
                }
            });
            sheets.value = lastResponse.value.data.data.content;
            totalPages.value = lastResponse.value.data.data.page.totalPages;
            totalElements.value = lastResponse.value.data.data.page.totalElements;
        } catch (error) {
            console.error("Error fetching sheets:", error);
        }
    };

    const createSheet = async () => {
        try {
            const formData = new FormData();
            formData.append("file", selectedSheet.value);

            await axios.post("excels/upload", formData, {
                requiresAuth: true,
                headers: {
                    "Content-Type": "multipart/form-data",
                }
            });
            await fetchSheets();
        } catch (error) {
            console.error("Error creating sheet:", error);
        }
    };

    const deleteSheet = async (sheetId) => {
        try {
            await axios.delete(`connection-fees/delete-by-task/${sheetId}`, {
                requiresAuth: true
            });
            await fetchSheets();
        } catch (error) {
            console.error("Error deleting sheets:", error);
        }
    };

    const saveSheet = async (sheetId) => {
        try {
            await axios.post(`connection-fees/${sheetId}`, {}, {
                requiresAuth: true
            });
            await fetchSheets();
        } catch (error) {
            console.error("Error deleting sheets:", error);
        }
    };

    function addShowProperty() {
        const stack = [...records.value];

        while (stack.length > 0) {
            const record = stack.pop();
            record.show = ref(false);
            const remainderChild = record.children.filter(child => child.status === 'REMINDER');

            if (record.children && record.children.length > 0) {
                if (remainderChild.length > 0) {
                    record.remainder = remainderChild[0].totalAmount;
                } else {
                    record.remainder = 0;
                }
                stack.push(...record.children);
            } else {
                record.remainder = record.totalAmount;
            }
        }
    }

    const getFees = async () => {
        records.value = undefined;
        const params = {
            sortBy: sortBy.value,
            sortDir: sortDir.value,
            page: currentPage.value,
            size: pageSize.value,
        }

        const dates = [
            'clarificationDateStart',
            'clarificationDateEnd',
            /*'changeDateStart',
            'changeDateEnd',*/
            'transferDateStart',
            'transferDateEnd',
            'extractionDateStart',
            'extractionDateEnd'
        ]
        const undefinedValues = [
            "აირჩიეთ რეგიონი",
            "აირჩიეთ მ/ც",
            "ორდერის სტატუსი",
            "ჩანაწერის სტატუსი"
        ]
        Object.entries(filter)
            .filter(([_, value]) => value && !undefinedValues.includes(value))
            .reduce((_, [key, value]) => {
                if (dates.includes(key)) {
                    params[key] = value
                    if (key.indexOf('extraction') === -1) {
                        params[key] += ` ${key.indexOf('Start') !== -1 ? '00' : '24'}:00:00.000000`
                    }
                } else {
                    params[key] = value;
                }
            }, {});

        if (params.orderN) {
            params.orderN = params.orderN
                .split(' ')
                .map(order => order.trim())
                .filter(order => order !== '');
        }

        try {
            lastResponse.value = await axios.get(`connection-fees/filter`, {
                params,
                paramsSerializer: (params) => qs.stringify(params, {arrayFormat: "comma"})
            });
            records.value = lastResponse.value.data.content;
            addShowProperty();
            totalPages.value = lastResponse.value.data.page.totalPages;
            totalElements.value = lastResponse.value.data.page.totalElements;
        } catch (error) {
            console.error("Error fetching sheets:", error);
        }
    };

    const formatDate = (dateString, includeTime = false) => {
        const date = new Date(dateString);
        const day = date.getDate().toString().padStart(2, '0');
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const year = date.getFullYear();
        if (includeTime) {
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            const seconds = date.getSeconds().toString().padStart(2, '0');
            return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
        } else {
            return `${day}.${month}.${year}`;
        }
    };

    const withdrawTypes = [
        '1 (პირველი გადახდა)',
        '2 (მეორე გადახდა)',
        '3 (სრული საფასურის გადახდა)',
        '4 (ერთანი გადახდა, გადანაწილებული რამოდენიმე პროექტის საფასურად)',
        '5 (სავარაუდოდ არაა ახალი მიერთების საფასური)',
        '6 (თანხის დაბრუნება)',
        '7 (გადანაწილებული გადახდა / რამოდენიმეჯერ გადახდა)',
        '8 (სააბონენტო ბარათზე თანხის დასმა)',
        '9 (ხაზის მშენებლობა / არარეგულირებული პროექტები (პირველი ან სრული გადახდა))',
        '10 (სისტემის ნებართვის საფასური)',
        '19 (ხაზის მშენებლობა / არარეგულირებული პროექტები (მეორე გადახდა))',
        '11 (მიწოდებიდან გადმოტანილი თანხა)',
        '12 (ჯარიმის გადატანა)',
        '13 (საპროექტო ტრასის შეტანხმება)',
        '14 (ჰესები DDSH)',
        '15 (ჰესები DDNA)',
        'null'
    ];


    const sortOptions = [
        {text: 'N კლებადი', by: 'id', dir: 'DESC'},
        {text: 'N ზრდადი', by: 'id', dir: 'ASC'},
        {text: 'ორდერის N კლებადი', by: 'orderN', dir: 'DESC'},
        {text: 'ორდერის N ზრდადი', by: 'orderN', dir: 'ASC'},
        {text: 'რეგიონი კლებადი', by: 'region', dir: 'DESC'},
        {text: 'რეგიონი ზრდადი', by: 'region', dir: 'ASC'},
        {text: 'მ/ც კლებადი', by: 'serviceCenter', dir: 'DESC'},
        {text: 'მ/ც ზრდადი', by: 'serviceCenter', dir: 'ASC'},
        {text: 'პროექტის N კლებადი', by: 'projectID', dir: 'DESC'},
        {text: 'პროექტის N ზრდადი', by: 'projectID', dir: 'ASC'},
        {text: 'ტიპი კლებადი', by: 'withdrawType', dir: 'DESC'},
        {text: 'ტიპი ზრდადი', by: 'withdrawType', dir: 'ASC'},
        {text: 'გარკვევის თარიღი კლებადი', by: 'clarificationDate', dir: 'DESC'},
        {text: 'გარკვევის თარიღი ზრდადი', by: 'clarificationDate', dir: 'ASC'},
        /*{text: 'ცვლილების თარიღი კლებადი', by: 'changeDate', dir: 'DESC'},
        {text: 'ცვლილების თარიღი ზრდადი', by: 'changeDate', dir: 'ASC'},*/
        {text: 'გადმოტანის თარიღი კლებადი', by: 'transferDate', dir: 'DESC'},
        {text: 'გადმოტანის თარიღი ზრდადი', by: 'transferDate', dir: 'ASC'},
        {text: 'ჩარიცხვის თარიღი კლებადი', by: 'extractionDate', dir: 'DESC'},
        {text: 'ჩარიცხვის თარიღი ზრდადი', by: 'extractionDate', dir: 'ASC'},
        {text: 'ბრუნვა კლებადი', by: 'totalAmount', dir: 'DESC'},
        {text: 'ბრუნვა თანხა ზრდადი', by: 'totalAmount', dir: 'ASC'},
    ];


    const sortByDir = ref(sortOptions[16]);

    const pageOptions = [
        10, 25, 50, 100, 250, 500, 1000
    ];

    watch(sortByDir, async (newSortDir) => {
        sortBy.value = newSortDir.by;
        sortDir.value = newSortDir.dir;
        currentPage.value = 1;
        await getFees();
    })

    watch(pageSize, async () => {
        currentPage.value = 1;
        await getFees();
    })

    return {
        sheets,
        currentPage,
        pageSize,
        totalPages,
        totalElements,
        selectedSheet,
        lastResponse,
        fetchSheets,
        createSheet,
        formatDate,
        deleteSheet,
        saveSheet,
        getFees,
        records,
        filter,
        sortBy,
        sortDir,
        sortByDir,
        withdrawTypes,
        sortOptions,
        pageOptions,
    };
}