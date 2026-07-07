<script setup>
import {computed, onMounted, ref, watch} from "vue";
import useCenters from "/src/composables/useCenters.js";
import useUploads from "/src/composables/useUploads.js";
import RecursiveRow from "/src/components/RecursiveRow.vue";
import {useAuthStore} from "/src/stores/auth.js";
import DateFilter from "../components/FilterDate.vue";
import UserSelect from "../components/UserSelect.vue";
import Pagination from "../components/Pagination.vue";
import Confirm from "../components/modals/Confirm.vue";
import {useFilterStore} from "/src/stores/filter.js";
import {useRoute} from "vue-router";
import axios from "/src/interceptors/axios";

const filterStore = useFilterStore();
const {filter} = filterStore;
const route = useRoute();

const {
  records,
  getFees,
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  withdrawTypes,
  sortOptions,
  sortByDir,
  formatDate,
} = useUploads()

const {
  getRegionsByParentId,
  _regions,
  _serviceCenters,
  updateRecord,
  extractionFee,
  handleEditClick,
  divide,
  sc,
  getServiceCenters,
  deleteRecord
} = useCenters()

const _error = ref(false)
const wasCleared = ref(false)

const handleClearClick = () => {
  wasCleared.value = true;
  _error.value = false;
  extractionFee.value.orderN = '';
  extractionFee.value.orderStatus = null;
  extractionFee.value.projectID = '';
  extractionFee.value.withdrawType = 'აირჩიეთ ტიპი';
  extractionFee.value.paymentOrderSentDate = null;
  extractionFee.value.treasuryRefundDate = null;
  extractionFee.value.clarificationDate = null;
  extractionFee.value.note = '';
  searchTerm.value = '';
  extractionFee.value.region = 'აირჩიეთ რეგიონი';
  extractionFee.value.serviceCenter = 'აირჩიეთ მ/ც';
  canceledProject.value = '';
};

const handleSaveClick = async () => {
  const {region, withdrawType, serviceCenter, projectID, orderN} = extractionFee.value;
  const isRefund = withdrawType === '6 (თანხის დაბრუნება)';
  const isAdmin = authStore.user.role === 'ROLE_ADMIN';

  // ორდერის N "N/A" — მ/ც და პროექტის N სავალდებულო აღარ არის (ნებისმიერი ტიპისთვის)
  const isTypeException = (orderN ?? '').trim().toUpperCase() === 'N/A';

  // --- Validation Checks ---
  const isRegionInvalid = !isRefund && region === 'აირჩიეთ რეგიონი';
  const isServiceCenterInvalid = !isRefund && serviceCenter === 'აირჩიეთ მ/ც';
  const isWithdrawTypeInvalid = withdrawType === 'აირჩიეთ ტიპი';
  const isProjectInvalid = !isAdmin && !projectID;

  const isInvalid = isRegionInvalid || isServiceCenterInvalid || isWithdrawTypeInvalid || isProjectInvalid;

  const performSaveActions = async () => {
    const trimmedString = canceledProject.value.trim();
    extractionFee.value.canceledProject = trimmedString === ''
        ? []
        : trimmedString.split(/\s+/);
    canceledProject.value = '';
    wasCleared.value = false;
    await updateRecord();
    await getFees();
    searchTerm.value = '';
    document.getElementById('my_modal_1').close();
  };

  const clearPlaceholders = () => {
    if (extractionFee.value.region === 'აირჩიეთ რეგიონი' || extractionFee.value.region === '') {
      extractionFee.value.region = '';
    }
    if (extractionFee.value.serviceCenter === 'აირჩიეთ მ/ც') {
      extractionFee.value.serviceCenter = '';
    }
    if (extractionFee.value.withdrawType === 'აირჩიეთ ტიპი') {
      extractionFee.value.withdrawType = '';
    }
  };

  if (wasCleared.value) {
    clearPlaceholders();
    await performSaveActions();
  } else if (isTypeException) {
    clearPlaceholders();
    // ეს თანხა გამოუყენებლად ვერ ჩაითვლება — სტატუსი ავტომატურად ხდება შევსებული
    if (!extractionFee.value.orderStatus || extractionFee.value.orderStatus === 'ORDER_INCOMPLETE') {
      extractionFee.value.orderStatus = 'ORDER_COMPLETE';
    }
    await performSaveActions();
  } else if (isInvalid) {
    if (canceledProject.value) {
      clearPlaceholders();
      await performSaveActions();
    } else {
      _error.value = true;
    }
  } else {
    await performSaveActions();
  }
};

const hdc = async () => {
  canceledProject.value = '';
  await deleteRecord();
  await getFees();
  document.getElementById('my_modal_1').close()
}
const hec = async (extraction) => {
  _error.value = false;
  searchTerm.value = extraction.serviceCenter;
  canceledProject.value = extraction.canceledProject.join(" ");
  await handleEditClick(extraction)
}

const id = ref()
const amount = ref('')
const remainder = ref();
const error = ref(false)
watch(amount, (newAmount) => {
  error.value = remainder.value < newAmount;
})


const handleDivideClick = async (args) => {
  document.getElementById('my_modal_7').showModal()
  id.value = args[0];
  remainder.value = args[1];
}


const handleDivision = async () => {
  if (remainder < amount.value) {
    error.value = true;
    return;
  }
  await divide(id.value, mapToArray())
  await getFees()
  id.value = undefined;
  amount.value = '';
  remainder.value = undefined;
  error.value = false;
}


onMounted(async () => {
  await getRegionsByParentId();
  await getFees();
  await getServiceCenters()
})

const regex = /^(\d+(\.\d+)?(\s\d+(\.\d+)?)*)$/;
const cleanAmount = () => amount.value.replace(/\s+/g, ' ').trim();
const mapToArray = () => cleanAmount().split(' ').map(Number);
const sumArray = () => mapToArray().reduce((a, b) => a + b, 0);
const validateAmount = () => {
  const numbers = mapToArray();
  return regex.test(cleanAmount()) && numbers.every(n => n > 0) && (sumArray() <= remainder.value);
};

const authStore = useAuthStore();

watch(currentPage, async (value) => {
      if (value > totalPages) {
        currentPage.value = totalPages.value;
      }
      if (value) {
        await getFees();
      }
    }
)

const searchTerm = ref('');
const isDropdownOpen = ref(false);

const filteredServiceCenters = computed(() =>
    sc.value.filter(center => center.name.toLowerCase().includes(searchTerm.value))
);

function selectCenter(centerName, parentName) {
  extractionFee.value.serviceCenter = centerName;
  searchTerm.value = centerName;
  extractionFee.value.region = parentName;
  isDropdownOpen.value = false;
}

function closeDropdown() {
  setTimeout(() => isDropdownOpen.value = false, 200);
}

const clearRegion = () => {
  filter.region = 'აირჩიეთ რეგიონი';
  _serviceCenters.value = [];
  clearSC();
}

const clearSC = () => {
  filter.serviceCenter = 'აირჩიეთ მ/ც';
}

const clearType = () => {
  filter.withdrawType = [];
}

const clearStatus = () => {
  filter.status = 'ჩანაწერის სტატუსი';
}

const clearOrderStatus = () => {
  filter.orderStatus = 'ორდერის სტატუსი';
}

const clearClarification = () => {
  filter.clarificationDateStart = undefined;
  filter.clarificationDateEnd = undefined;
}

/*const clearChange = () => {
  filter.changeDateStart = undefined;
  filter.changeDateEnd = undefined;
}*/

const clearTransfer = () => {
  filter.transferDateStart = undefined;
  filter.transferDateEnd = undefined;
}

const clearExtraction = () => {
  filter.extractionDateStart = undefined;
  filter.extractionDateEnd = undefined;
}

const clearSortByDir = () => {
  sortByDir.value = sortOptions[16];
}

const handleClear = async () => {
  filterStore.clearFilter();
  _serviceCenters.value = [];
  clearSortByDir();
  await handleFilter();
}

const handleFilter = async () => {
  currentPage.value = 1;
  await getFees();
}

async function getSelectedParentId(event) {
  await getRegionsByParentId(Number(event.target.selectedOptions[0].getAttribute('data-id')));
}

const isVisible = ref(false);

const toggleType = (type) => {
  const index = filter.withdrawType.indexOf(type);
  if (index === -1) {
    filter.withdrawType.push(type);
  } else {
    filter.withdrawType.splice(index, 1);
  }
};

const isTypeDropdownOpen = ref(false);
const closeTypeDropdown = () => {
  isTypeDropdownOpen.value = false;
}

function extractStartingNumber(type) {
  if (type === 'null') {
    return type;
  } else {
    const match = type.match(/^\d+/);
    return match[0]
  }
}

const canceledProject = ref('');

const similarPayments = ref([]);
const similarPage = ref(1);
const similarPageSize = ref(10);
const similarTotalPages = ref(0);
const similarTotalElements = ref(0);
const similarLoading = ref(false);

const fetchSimilarPayments = async () => {
  if (!extractionFee.value || !extractionFee.value.tax) return;
  similarLoading.value = true;
  try {
    const {data} = await axios.get(`connection-fees/by-taxpayer`, {
      params: {
        taxPayerId: extractionFee.value.tax,
        page: similarPage.value,
        size: similarPageSize.value
      }
    });
    similarPayments.value = data.content;
    similarTotalPages.value = data.page.totalPages;
    similarTotalElements.value = data.page.totalElements;
  } catch (e) {
    console.error(e);
  } finally {
    similarLoading.value = false;
  }
}

watch(similarPage, () => {
  fetchSimilarPayments();
});

watch(similarPageSize, () => {
  if (similarPage.value === 1) {
    fetchSimilarPayments();
  } else {
    similarPage.value = 1;
  }
});
</script>

<template>
  <!-- Toggle using an icon (Chevron) -->
  <div @click="isVisible = !isVisible" class="cursor-pointer flex items-center mb-2.5">
    <span class="mr-2">
      <i :class="isVisible ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
    </span>
    <span class="font-bold text-sm">ფილტრები</span> <!-- Optional label if you want to have text as well -->
  </div>

  <transition>
    <!--ფილტრი-->
    <div v-if="isVisible" class="flex flex-col gap-y-5">
      <!--პირველი ხაზი-->
      <div class="filter-row">
        <!--რეგიონი-->
        <div class="select-container">
          <select class="filter-select" v-model="filter.region" @change="getSelectedParentId">
            <option disabled selected>აირჩიეთ რეგიონი</option>

            <option :value="region.name" v-for="(region, index) in _regions" v-text="region.name" :data-id="region.id"
                    :key="index"/>
          </select>

          <button class="filter-clear-btn" @click="clearRegion">✕</button>
        </div>
        <!--რეგიონი-->

        <!--მ/ც-->
        <div class="select-container">
          <select class="filter-select" v-model="filter.serviceCenter">
            <option disabled selected>აირჩიეთ მ/ც</option>

            <option :value="center.name" v-for="(center, index) in _serviceCenters" v-text="center.name" :key="index"/>
          </select>

          <button class="filter-clear-btn" @click="clearSC">✕</button>
        </div>
        <!--მ/ც-->

        <!--ტიპი-->
        <div class="relative select-container">
          <input type="text" @click="isTypeDropdownOpen = !isTypeDropdownOpen" placeholder="აირჩიეთ ტიპი"
                 class="input input-bordered input-sm focus:outline-0 filter-select cursor-pointer" readonly/>

          <button class="filter-clear-btn" @click="clearType(); isTypeDropdownOpen = false">✕</button>

          <div v-if="isTypeDropdownOpen"
               class="absolute z-10 bg-white border border-gray-300 rounded-md shadow-lg max-w-xs top-9">
            <div v-for="(type, index) in withdrawTypes" :key="index"
                 class="p-2 hover:bg-gray-100 cursor-pointer text-sm">
              <label class="flex items-center space-x-2">
                <input type="checkbox" class="checkbox checkbox-xs" :value="extractStartingNumber(type)"
                       :checked="filter.withdrawType.includes(extractStartingNumber(type))"
                       @change="toggleType(extractStartingNumber(type))"/>
                <span>{{ (index !== 16) ? type : "უცნობი ტიპი" }}</span>
              </label>
            </div>
          </div>
        </div>
        <!--ტიპი-->

        <!--სტატუსი-->
        <div class="flex">
          <div class="select-container" title="ორდერის სტატუსი">
            <select class="filter-select" v-model="filter.orderStatus">
              <option disabled selected>ორდერის სტატუსი</option>

              <option value="ORDER_INCOMPLETE">გამოუყენებელი</option>

              <option value="ORDER_COMPLETE">შევსებული</option>

              <option value="YELLOW_AMOUNT">დასასრულებელი</option>

              <option value="CANCELED">გაუქმებული</option>

              <option value="IN_PROGRESS">პროცესში</option>

              <option value="RETURNED">დაბრუნებული</option>

              <!--<option value="SOFT_DELETED">წაშლილები</option>-->
            </select>

            <button class="filter-clear-btn" @click="clearOrderStatus">✕</button>
          </div>


          <div class="select-container" title="ჩანაწერის სტატუსი">
            <select class="filter-select" v-model="filter.status">
              <option disabled selected>ჩანაწერის სტატუსი</option>

              <option value="TRANSFER_COMPLETE">შევსებული</option>

              <option value="TRANSFERRED">შესავსები</option>

              <option value="CANCELED">გაუქმებული</option>

              <!--<option value="SOFT_DELETED">წაშლილები</option>-->
            </select>

            <button class="filter-clear-btn" @click="clearStatus">✕</button>
          </div>
        </div>
        <!--სტატუსი-->

        <!--ბრუნვა-->
        <div class="grid grid-cols-3 items-center gap-x-8">
          <p class="filter-label">ბრუნვა (კრედ)</p>

          <input type="text" class="filter-input" placeholder="მინ" v-model="filter.totalAmountStart"/>

          <input type="text" class="filter-input" placeholder="მაქს" v-model="filter.totalAmountEnd"/>
        </div>
        <!--ბრუნვა-->
      </div>
      <!--პირველი ხაზი-->

      <!--მეორე ხაზი-->
      <div class="filter-row">
        <!--ორდერის N-->
        <input type="text" class="filter-input" v-model="filter.orderN"
               placeholder="ორდერის N (მრავლობითი: 123 456 789)"/>
        <!--ორდერის N-->

        <!--პროექტის N-->
        <input type="text" class="filter-input" v-model="filter.projectID" placeholder="პროექტის N"/>
        <!--პროექტის N-->

        <!--N-->
        <input type="text" class="filter-input" v-model="filter.id" placeholder="N"/>
        <!--N-->

        <!--დანიშნულება-->
        <input type="text" class="filter-input" v-model="filter.purpose" placeholder="დანიშნულება"/>
        <!--დანიშნულება-->

        <!--ID-->
        <input type="text" class="filter-input" v-model="filter.tax" placeholder="ID"/>
        <!--ID-->
      </div>
      <!--მეორე ხაზი-->

      <div class="filter-row">
        <!--დამატებითი ინფორმაცია-->
        <input type="text" class="input input-bordered input-sm focus:outline-0 w-[86%]" v-model="filter.description"
               placeholder="დამატებითი ინფორმაცია"/>
        <!--დამატებითი ინფორმაცია-->

        <user-select v-model="filter.change_person"/>

        <date-filter v-model:start-date="filter.clarificationDateStart" v-model:end-date="filter.clarificationDateEnd"
                     label="გარკვევის თარიღი" @clear="clearClarification"/>

        <!--<date-filter v-model:start-date="filter.changeDateStart" v-model:end-date="filter.changeDateEnd"
          label="ცვლილების თარიღი" @clear="clearChange" />-->

        <date-filter v-model:start-date="filter.transferDateStart" v-model:end-date="filter.transferDateEnd"
                     label="გადმოტანის თარიღი" @clear="clearTransfer"/>

        <date-filter v-model:start-date="filter.extractionDateStart" v-model:end-date="filter.extractionDateEnd"
                     label="ჩარიცხვის თარიღი" @clear="clearExtraction"/>
      </div>

      <!--მესამე ხაზი-->
      <div class="filter-row">
        <input type="text" class="input input-bordered w-[85%] input-sm focus:outline-0" v-model="filter.note"
               placeholder="შენიშვნა"/>

        <div class="select-container">
          <select class="select select-bordered select-sm w-full focus:outline-0" v-model="sortByDir">
            <option disabled>დალაგება</option>
            <option :value="option" v-for="(option, index) in sortOptions" v-text="option.text" :key="index"/>
          </select>
          <button class="filter-clear-btn" @click="clearSortByDir">✕</button>
        </div>


        <!--History Id-->
        <input type="text" class="filter-input" v-model="filter.history" placeholder="History Id"/>
        <!--History Id-->

        <div class="flex gap-x-2">
          <button class="btn btn-neutral btn-sm" @click="handleFilter">გაფილტვრა</button>
          <button class="btn btn-neutral btn-sm" @click="handleClear">გასუფთავება</button>
        </div>
      </div>
      <!--მესამე ხაზი-->
    </div>
    <!--ფილტრი-->
  </transition>

  <!--ცხრილი-->
  <div class="flex flex-col gap-y-5 mt-5">
    <table class="table table-xs">
      <thead>
      <tr>
        <th v-if="authStore.user"/>

        <th>N</th>

        <th>ორდერის N</th>

        <th>რეგიონი</th>

        <th>მ/ც</th>

        <th>პროექტის N</th>

        <th>ტიპი</th>

        <th>ბრუნვა (კრედ)</th>

        <th>დანიშნულება</th>

        <th>დამატებითი ინფორმაცია</th>

        <th>ID</th>

        <th>ჩარიცხვის თარიღი</th>

        <th>გარკვევის თარიღი</th>

        <!--<th>ცვლილების თარიღი</th>-->

        <th>თანხის დაბრუნების <br/> მოთხოვნის თარიღი</th>

        <th>საგადახდო დავალების <br/> მიღების თარიღი</th>

        <th>History Id</th>

        <th>შემცვლელი</th>
      </tr>
      </thead>
      <tbody v-if="records && records.length > 0">
      <RecursiveRow @handleDivideClick="handleDivideClick" @handleEditClick="hec" :records="records"/>
      </tbody>

      <tbody v-else-if="records && records.length === 0">
      <tr>
        <td>
          ჩანაწერები ვერ მოიძებნა!
        </td>
      </tr>
      </tbody>

      <tbody v-else>
      <tr>
        <td class="flex items-center gap-x-2.5">
          იტვირთება <span class="loading loading-spinner loading-md"/>
        </td>
      </tr>
      </tbody>
    </table>
    <!--ცხრილი-->

    <pagination v-model:current-page="currentPage" v-model:page-size="pageSize" v-model:total-pages="totalPages"
                v-model:total-elements="totalElements"/>
  </div>

  <dialog id="my_modal_1" class="modal">
    <div class="modal-box max-w-[45vw] flex flex-col gap-y-3 text-sm" v-if="extractionFee">
      <div class="flex gap-x-20">
        <!-- Left Column -->
        <div class="flex flex-col w-1/2 gap-y-2">
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">ორდერის N</label>
            <div class="grid grid-cols-2 gap-x-2 max-w-xs">
              <input type="text" class="input input-bordered w-full max-w-xs input-sm focus:outline-0"
                     v-model="extractionFee.orderN"/>
              <select class="filter-select" v-model="extractionFee.orderStatus">
                <!--<option disabled selected>აირჩიეთ ორდერის ტიპი</option>-->

                <option value="ORDER_INCOMPLETE">გამოუყენებელი</option>

                <option value="ORDER_COMPLETE">შევსებული</option>

                <option value="YELLOW_AMOUNT">დასასრულებელი</option>

                <option value="CANCELED">გაუქმებული</option>

                <option value="IN_PROGRESS">პროცესში</option>

                <option value="RETURNED">დაბრუნებული</option>
              </select>
            </div>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">რეგიონი</label>
            <input :value="extractionFee.region" class="input input-bordered max-w-xs input-sm mb-2 focus:outline-0"
                   disabled>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">მ/ც</label>
            <div class="relative max-w-xs">
              <input type="text" v-model="searchTerm" placeholder="აირჩიეთ მ/ც" @focus="isDropdownOpen = true"
                     @blur="closeDropdown" class="input input-bordered input-sm mb-2 focus:outline-0 w-full"/>

              <!-- Dropdown options -->
              <div v-if="isDropdownOpen && filteredServiceCenters.length"
                   class="absolute z-10 bg-white border border-gray-300 rounded-md shadow-lg mt-1">
                <div v-for="(center, index) in filteredServiceCenters" :key="index"
                     @mousedown="selectCenter(center.name, center.parent.name)"
                     class="p-2 hover:bg-gray-100 cursor-pointer">
                  {{ center.name }}
                </div>
              </div>
            </div>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">პროექტის N</label>
            <input type="text" class="input input-bordered w-full max-w-xs input-sm focus:outline-0"
                   v-model="extractionFee.projectID"/>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">ტიპი</label>
            <select class="select select-bordered select-sm w-full max-w-xs focus:outline-0"
                    v-model="extractionFee.withdrawType">
              <option disabled selected>აირჩიეთ ტიპი</option>
              <option :value="extractStartingNumber(type)" v-for="(type, index) in withdrawTypes.slice(0, 16)"
                      v-text="type" :key="index"/>
            </select>
          </div>
          <div v-if="extractionFee.withdrawType === '6'" class="flex items-center gap-x-2.5">
            <input type="date" class="text-sm" v-model="extractionFee.paymentOrderSentDate"/>

            <input type="date" class="text-sm" v-model="extractionFee.treasuryRefundDate"/>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">შენიშვნა</label>
            <textarea v-if="extractionFee"
                      class="textarea textarea-bordered textarea-xs w-full max-w-xs focus:outline-0 resize-none"
                      v-model="extractionFee.note"></textarea>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">გაუქმებული პროექტები</label>
            <textarea v-if="extractionFee"
                      placeholder="პროექტები გამოყავით სფეისით"
                      class="textarea textarea-bordered textarea-xs w-full max-w-xs focus:outline-0 resize-none"
                      v-model="canceledProject"></textarea>
          </div>
        </div>

        <!-- Right Column -->
        <div class="flex flex-col justify-between w-1/2 gap-y-2">
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">გარკვევის თარიღი</label>
            <div v-text="extractionFee.clarificationDate ? formatDate(extractionFee.clarificationDate) : ''"/>
          </div>
          <!--<div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">ცვლილების თარიღი</label>
            <div>
              <p v-text="extractionFee.changeDate ? formatDate(extractionFee.changeDate, true) : ''" />
              <p class="text-neutral underline font-bold" v-if="extractionFee.changeDate"
                v-text="`${extractionFee.changePerson?.firstName} ${extractionFee.changePerson?.lastName}`" />
            </div>
          </div>-->
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">გადმოტანის თარიღი</label>
            <div>
              <p v-text="extractionFee.transferDate ? formatDate(extractionFee.transferDate, true) : ''"/>
              <p v-if="extractionFee.transferPerson" class="text-neutral underline font-bold"
                 v-text="`${extractionFee.transferPerson.firstName} ${extractionFee.transferPerson.lastName}`"/>
            </div>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">ჩარიცხვის თარიღი</label>
            <div v-text="extractionFee.extractionDate ? formatDate(extractionFee.extractionDate) : ''"/>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">ბრუნვა</label>
            <div v-text="extractionFee.totalAmount"/>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">დანიშნულება</label>
            <div v-text="extractionFee.purpose"/>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">დამატებიტი ინფორმაცია</label>
            <div v-text="extractionFee.description"/>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">თანხის დაბრუნებაზე ხაზინაში მოთხოვნის გაგზავნის თარიღი</label>
            <div
                v-text="extractionFee.paymentOrderSentDate ? formatDate(extractionFee.paymentOrderSentDate) : ''"/>
          </div>
          <div class="flex flex-col gap-y-2">
            <label class="font-semibold text-gray-600">საგადახდო დავალების გაგზავნის თარიღი</label>
            <div v-text="extractionFee.treasuryRefundDate ? formatDate(extractionFee.treasuryRefundDate) : ''"/>
          </div>
        </div>
      </div>

      <span class="text-error" v-if="_error">გთხოვთ შეავსოთ ყველა სავადლებულო ველი!</span>

      <div class="flex items-center justify-between mt-4">
        <div class="flex justify-between items-center">
          <div class="modal-action">
            <button class="btn btn-neutral" @click="handleSaveClick">შენახვა</button>

            <button class="btn btn-warning" @click="handleClearClick">გასუფთავება</button>

            <!--            <button class="btn btn-info text-white" -->
            <!--                    @click="similarPage = 1; fetchSimilarPayments()" -->
            <!--                    onclick="similar_payments_modal.showModal()">-->
            <!--              მსგავსი ჩარიცხვები-->
            <!--            </button>-->

            <button class="btn btn-error text-white"
                    onclick="document.getElementById('delete_transaction_modal').showModal();">წაშლა
            </button>
          </div>
        </div>

        <button class="btn" @click="searchTerm = ''; canceledProject = ''; wasCleared = false;" onclick="my_modal_1.close()">გაუქმება
        </button>
      </div>
    </div>
  </dialog>

  <confirm id="delete_transaction_modal" @accept="hdc"/>

  <dialog id="my_modal_7" class="modal">
    <div class="modal-box">
      <form method="dialog">
        <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2 focus:outline-none">✕</button>
      </form>
      <h3 class="text-lg font-bold" v-text="`გთხოვთ შეიყვანოთ თანხა! ნაშთი ${remainder}`"/>
      <div class="modal-action items-center">
        <input class="input input-bordered w-full max-w-xs input-sm focus:outline-0" type="text" v-model="amount"
               placeholder="10 25 50"/>
        <form method="dialog" class="flex items-center justify-end w-full gap-x-5">
          <button class="btn btn-neutral btn-sm" v-text="'გაყოფა'" :disabled="!validateAmount()"
                  @click="handleDivision"/>
          <button class="btn btn-sm" v-text="'გაუქმება'"/>
        </form>
      </div>
    </div>
  </dialog>

  <dialog id="similar_payments_modal" class="modal">
    <div class="modal-box w-[80vw] h-[80vh] max-w-none flex flex-col">
      <!--      <div class="flex justify-between items-center mb-4">-->
      <!--        <h3 class="font-bold text-lg">მსგავსი ჩარიცხვები</h3>-->
      <!--        <form method="dialog">-->
      <!--          <button class="btn btn-sm btn-circle btn-ghost">✕</button>-->
      <!--        </form>-->
      <!--      </div>-->

      <div v-if="similarLoading" class="flex justify-center my-4 flex-grow">
        <span class="loading loading-spinner loading-lg"></span>
      </div>

      <div v-else class="overflow-y-auto flex-grow">
        <table class="table table-xs table-pin-rows">
          <thead>
          <tr>
            <th>ID</th>
            <th>რეგიონი</th>
            <th>მ/ც</th>
            <th>Tax ID</th>
            <th>თანხა</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="payment in similarPayments" :key="payment.id">
            <td>{{ payment.id }}</td>
            <td>{{ payment.region }}</td>
            <td>{{ payment.serviceCenter }}</td>
            <td>{{ payment.tax }}</td>
            <td>{{ payment.totalAmount }}</td>
          </tr>
          <tr v-if="similarPayments.length === 0">
            <td colspan="5" class="text-center">ჩანაწერები არ მოიძებნა</td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="mt-4">
        <pagination
            :absolute="true"
            v-model:current-page="similarPage"
            v-model:page-size="similarPageSize"
            v-model:total-pages="similarTotalPages"
            v-model:total-elements="similarTotalElements"
        />
      </div>
    </div>
  </dialog>
</template>

<style scoped>
/* we will explain what these classes do next! */
.v-enter-active,
.v-leave-active {
  transition: opacity 0.3s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}
</style>