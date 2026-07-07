import {ref, watch} from "vue";
import axios from "/src/interceptors/axios";

export default function useFiles() {
    const files = ref([]);
    const selectedFile = ref(null);
    const currentPage = ref(1);
    const pageSize = ref(20);
    const totalPages = ref(1);
    const totalElements = ref(20);

    // Watcher for currentPage
    watch(currentPage, async (value) => {
        if (value > totalPages.value) {
            currentPage.value = totalPages.value;
        }
        if (value) {
            await getFiles();
        }
    });

    // Watcher for pageSize
    watch(pageSize, async () => {
        currentPage.value = 1;
        await getFiles();
    });

    // Fetch sheets from the server
    const getFiles = async () => {
        try {
            files.value = undefined;
            const {data} = await axios.get('extraction-task/all-upls', {
                params: {
                    page: currentPage.value,
                    size: pageSize.value,
                }
            });
            files.value = data.data.content;
            totalPages.value = data.data.page.totalPages;
            totalElements.value = data.data.page.totalElements;
        } catch (error) {
            console.error("Error fetching files:", error);
        }
    };

    // Create a new sheet
    const createFile = async () => {
        try {
            const formData = new FormData();
            formData.append("file", selectedFile.value);

            await axios.post("excels/upload", formData, {
                requiresAuth: true,
                headers: {
                    "Content-Type": "multipart/form-data",
                }
            });
            await getFiles();
        } catch (error) {
            console.error("Error creating sheet:", error);
        }
    };

    // Delete a sheet by ID
    const deleteFile = async (fileId) => {
        try {
            await axios.delete(`connection-fees/delete-by-task/${fileId}`, {
                requiresAuth: true
            });
            document.getElementById('delete_file_modal').close();
            await getFiles();
        } catch (error) {
            console.error("Error deleting sheet:", error);
        }
    };

    // Transfer a sheet by ID
    const transferFile = async (fileId) => {
        try {
            await axios.post(`connection-fees/${fileId}`, {}, {
                requiresAuth: true
            });
            document.getElementById('transfer_file_modal').close();
            await getFiles();
        } catch (error) {
            console.error("Error saving sheet:", error);
        }
    };

    return {
        files,
        currentPage,
        pageSize,
        totalPages,
        totalElements,
        selectedFile,
        getFiles,
        createFile,
        deleteFile,
        transferFile,
    };
}
