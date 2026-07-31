<script setup>
import useUploads from "/src/composables/useUploads";
import {onMounted, ref, watch} from "vue";
import useSheet from "/src/composables/useSheet.js";
import FileInput from "/src/components/uploads/FileInput.vue"
import {useAuthStore} from "/src/stores/auth.js";
import Pagination from "/src/components/Pagination.vue";
import useFiles from "/src/composables/useFiles.js";
import Confirm from "../components/modals/Confirm.vue";
import useNavigation from "../composables/useNavigation.js";

const {downloadFile} = useNavigation()

const authStore = useAuthStore();

const {formatDate} = useUploads();

const {
  files,
  selectedFile,
  currentPage,
  pageSize,
  totalPages,
  totalElements,
  getFiles,
  createFile,
  deleteFile,
  transferFile,
} = useFiles();
const {

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
} = useSheet()

const handleFilter = async () => {
  _currentPage.value = 1;
  await fetchSheetData();
}

const handleClear = async () => {
  clearFilter();
  await handleFilter();
}

const deleteId = ref();
const transferId = ref();

watch(currentPage, async (value) => {
  if (value > totalPages) {
    currentPage.value = totalPages.value;
  }
  if (value) {
    await getFiles()
  }
})

onMounted(async () => {
  await getFiles();
});
</script>

<template>
  <file-input v-if="authStore.user" v-model="selectedFile" @createFile="createFile"/>

  <confirm id="delete_file_modal" question="ნამდვილად გსურთ ფაილის წაშლა?" @accept="deleteFile(deleteId)"/>

  <confirm id="transfer_file_modal" question="ნამდვილად გსურთ ყველა ჩანაწერის გადატანა?"
           @accept="transferFile(transferId)"/>

  <div class="flex flex-col gap-y-5">
    <table class="table table-xs">
      <thead>
      <tr>
        <th>ფაილის სახელი</th>
        <th>ატვირთვის თარიღი</th>
        <th>ფაილის სტატუსი</th>
        <th>ფაილის ნახვა</th>
        <th v-if="authStore.user">ფაილის წაშლა</th>
        <th v-if="authStore.user">ბაზაში გადატანა</th>
        <th v-if="authStore.user">ფაილის ჩამტვირთვა</th>
      </tr>
      </thead>

      <tbody v-if="files && files.length > 0">
      <tr v-for="(sheet, index) in files" :key="index">
        <td>
          <div class="flex items-center gap-x-2.5">
            <img src="/src/assets/excel.svg.png" alt="excel icon" class="w-8"/>
            {{ sheet.fileName.split('\\').at(-1) }}
          </div>
        </td>
        <td v-text="formatDate(sheet.date)"/>
        <td>
          <img src="/src/assets/check_circle.svg" alt="check icon"
               v-if="sheet.status === 'GOOD' || sheet.status === 'TRANSFERRED_GOOD'"/>
          <img src="/src/assets/warning.svg" alt="warning icon"
               v-else-if="sheet.status === 'WARNING' || sheet.status === 'TRANSFERRED_WARNING'"/>
        </td>
        <td>
          <img src="/src/assets/visibility.svg" alt="view icon" class="cursor-pointer"
               @click="recordId = sheet.id; fetchSheetData()" onclick="my_modal_1.showModal(); "/>
        </td>
        <td v-if="authStore.user">
          <img v-if="sheet.status !== 'HISTORY'" src="/src/assets/delete.svg" alt="delete icon" @click="deleteId = sheet.id"
               onclick="delete_file_modal.showModal()" class="cursor-pointer"/>
        </td>
        <td v-if="authStore.user">
          <button v-if="sheet.status === 'GOOD' || sheet.status === 'WARNING'"
                  onclick="transfer_file_modal.showModal()">
            <img src="/src/assets/save.svg" alt="save icon" @click="transferId = sheet.id"/>
          </button>
        </td>
        <td v-if="authStore.user">
          <button
              v-if="sheet.status !== 'HISTORY'"
              class="cursor-pointer"
              @click="downloadFile(sheet.fileName.split('\\').at(-1))"
          >
            <img
                src="/src/assets/download.png"
                width="32"
                height="32"
                alt="download icon"
            />
          </button>
        </td>
      </tr>
      </tbody>

      <tbody v-else-if="files && files.length === 0">
      <tr>
        <td>
          გთხოვთ ატვირთოთ ფაილები!
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

    <pagination v-model:current-page="currentPage" v-model:page-size="pageSize" v-model:total-pages="totalPages"
                v-model:total-elements="totalElements"/>
  </div>

  <dialog id="my_modal_1" class="modal">
    <div class="modal-box max-w-7xl pt-8">
      <div class="overflow-x-auto space-y-4">
        <div class="flex items-center justify-between px-4 font-semibold">
          <p>ჯამური თანხა - {{ details.amount }}</p>
          <p>ჩანაწერი - {{ details.total }}</p>
          <p>უხარვეზო - {{ details.ok }}</p>
          <p>დახარვეზებული - {{ details.warning }}</p>
        </div>
        <div class="flex gap-x-16">
          <table class="table table-xs">
            <thead>
            <tr>
              <th>N</th>
              <th>ჩარიცხვის თარიღი</th>
              <th>სრული თანხა</th>
              <th>გადამხდელი</th>
              <th>დანიშნულება</th>
              <th>აღწერა</th>
              <th>სტატუსი</th>
            </tr>
            </thead>
            <tbody v-if="sheet && sheet.length > 0">
            <tr v-for="(extraction, index) in sheet" :key="index">
              <td v-text="extraction.id"/>
              <td v-text="formatDate(extraction.date)"/>
              <td v-text="extraction.totalAmount"/>
              <td v-text="extraction.tax"/>
              <td v-text="extraction.purpose"/>
              <td v-text="extraction.description"/>
              <td>
                <img src="/src/assets/check_circle.svg" alt="check icon" v-if="extraction.status === 'GOOD'"/>
                <img src="/src/assets/warning.svg" alt="warning icon" v-else/>
              </td>
            </tr>
            </tbody>

            <tbody v-else-if="sheet && sheet.length === 0">
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

          <div class="flex flex-col gap-y-10 font-medium">
            <div class="flex flex-col gap-y-2.5">
              <p>ჩარიცხვის თარიღი</p>

              <div class="flex flex-col gap-y-2">
                <div class="flex items-center gap-x-2">
                  <input type="date" class="text-sm" v-model="filter.startDate"/>
                </div>

                <div class="flex items-center gap-x-2">
                  <input type="date" class="text-sm" v-model="filter.endDate"/>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-y-2">
              <p>სრული თანხა</p>

              <div class="flex flex-col gap-y-5">
                <input type="text" class="input input-bordered input-sm focus:outline-0" placeholder="მინ"
                       v-model="filter.totalAmountStart"/>

                <input type="text" class="input input-bordered input-sm focus:outline-0" placeholder="მაქს"
                       v-model="filter.totalAmountEnd"/>
              </div>
            </div>

            <div class="flex flex-col gap-y-2">
              <p>სტატუსი</p>

              <div class="flex flex-col gap-y-2.5">
                <div class="flex items-center gap-x-2">
                  <input type="radio" name="radio" value="" class="radio radio-xs focus:outline-none" checked="checked"
                         v-model="filter.status"/>
                  <span class="text-xs">ყველა</span>
                </div>

                <div class="flex items-center gap-x-2">
                  <input type="radio" name="radio" value="GOOD" class="radio radio-xs focus:outline-none"
                         v-model="filter.status"/>
                  <span class="text-xs">უხარვეზო</span>
                </div>

                <div class="flex items-center gap-x-2">
                  <input type="radio" name="radio" value="WARNING" class="radio radio-xs focus:outline-none"
                         v-model="filter.status"/>
                  <span class="text-xs">დახარვეზებული</span>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-x-2 gap-y-2">
              <button class="btn btn-neutral btn-sm" @click="handleFilter">გაფილტვრა</button>
              <button class="btn btn-neutral btn-sm" @click="handleClear">გასუფთავება</button>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-action">
        <form method="dialog" @submit="_currentPage = 1; sheet = null; clearFilter();">
          <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">
            ✕
          </button>
        </form>
        <form method="dialog" @submit="_currentPage = 1; sheet = null; clearFilter();">
          <button class="btn">დახურვა</button>
        </form>
      </div>
    </div>

    <pagination v-model:current-page="_currentPage"
                v-model:page-size="_pageSize" v-model:total-pages="_totalPages"
                v-model:total-elements="_totalElements"/>
  </dialog>
</template>
