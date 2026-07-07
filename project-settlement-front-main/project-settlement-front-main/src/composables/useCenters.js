import {ref} from "vue";
import axios from "/src/interceptors/axios";

export default function useCenters() {
    const regions = ref();
    const _regions = ref();
    const serviceCenters = ref();
    const _serviceCenters = ref()
    const extractionFee = ref({
            id: '',
            orderN: '',
            region: '',
            serviceCenter: '',
            projectID: '',
            withdrawType: '',
            note: '',
            clarificationDate: '',
            /*changeDate: '',*/
            transferDate: '',
            extractionDate: '',
            totalAmount: '',
            purpose: '',
            description: '',
            orderStatus: '',
            canceledProject: [],
        }
    )
    const sc = ref([])

    const handleEditClick = async (extraction) => {
        extractionFee.value = {...extraction};
        document.getElementById('my_modal_1').showModal();
        if (!extractionFee.value.region) {
            extractionFee.value.region = 'აირჩიეთ რეგიონი';
        } else {
            await getRegionsByParentId(regions.value.find(reg => reg.name === extraction.region).id)
        }

        if (!extractionFee.value.serviceCenter) {
            extractionFee.value.serviceCenter = 'აირჩიეთ მ/ც';
        }

        if (!extractionFee.value.withdrawType) {
            extractionFee.value.withdrawType = 'აირჩიეთ ტიპი';
        }
    }

    const getRegionsByParentId = async (parentId = 68) => {
            try {
                const {data} = await axios.get(`business-units/by-parent/${parentId}`);
                if (parentId === 68) {
                    regions.value = data.data;
                    _regions.value = data.data
                } else {
                    serviceCenters.value = data.data;
                    _serviceCenters.value = data.data;
                }
            } catch
                (error) {
                console.error("Error fetching data:", error);
            }
        }
    ;

    const getServiceCenters = async () => {
        try {
            const {data} = await axios.get('business-units/unit-key/62');
            sc.value = data;
        } catch
            (error) {
            console.error("Error service centers data:", error);
        }
    }

    const updateRecord = async () => {
        try {
            delete extractionFee.value.changePerson
            delete extractionFee.value.transferPerson
            if (!extractionFee.value.note) {
                extractionFee.value.note = '';
            }
            if (!extractionFee.value.projectID) {
                extractionFee.value.projectID = '';
            }
            if (!extractionFee.value.orderStatus) {
                extractionFee.value.orderStatus = null;
            }
            
            await axios.put(`connection-fees/${extractionFee.value.id}`, extractionFee.value, {requiresAuth: true})
            extractionFee.value = undefined;
        } catch (error) {
            console.error("Error updating fee:", error);
        }
    }


    const deleteRecord = async () => {
        try {
            await axios.delete(`connection-fees/soft-delete/${extractionFee.value.id}`, {requiresAuth: true});
            extractionFee.value = undefined;
        } catch (error) {
            console.log(error)
        }
    }

    const divide = async (id, amounts) => {
        try {
            await axios.post(`connection-fees/divide-fee/${id}`, amounts, {requiresAuth: true})
        } catch (error) {
            console.log(error)
        }
    }


    return {
        getRegionsByParentId,
        regions,
        _regions,
        serviceCenters,
        _serviceCenters,
        updateRecord,
        extractionFee,
        handleEditClick,
        deleteRecord,
        divide,
        sc,
        getServiceCenters
    };
}
