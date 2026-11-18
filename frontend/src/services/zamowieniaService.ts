import axios from 'axios';
import type {
    StatusZamowienia,
    Zamowienie,
    ZamowienieCreate,
    ZamowienieUpdate,
} from '../types/zamowienie';

const API_URL = '/api/v1/zamowienia';

export const zamowieniaService = {
    getAll: async (params?: {
        id_klienta?: number;
        status_zamowienia?: StatusZamowienia;
    }): Promise<Zamowienie[]> => {
        const response = await axios.get(API_URL, { params });
        return response.data;
    },

    getById: async (id: number): Promise<Zamowienie> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (zamowienie: ZamowienieCreate): Promise<Zamowienie> => {
        const response = await axios.post(API_URL, zamowienie);
        return response.data;
    },

    update: async (id: number, zamowienie: ZamowienieUpdate): Promise<Zamowienie> => {
        const response = await axios.put(`${API_URL}/${id}`, zamowienie);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
