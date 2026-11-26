import axios from 'axios';
import type {
    Kategoria,
    KategoriaCreate,
    KategoriaUpdate,
} from '../types/kategoria';

const API_URL = '/api/v1/kategorie';

export const kategorieService = {
    getAll: async (): Promise<Kategoria[]> => {
        const response = await axios.get(API_URL);
        return response.data;
    },

    getById: async (id: number): Promise<Kategoria> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (kategoria: KategoriaCreate): Promise<Kategoria> => {
        const response = await axios.post(API_URL, kategoria);
        return response.data;
    },

    update: async (id: number, kategoria: KategoriaUpdate): Promise<Kategoria> => {
        const response = await axios.put(`${API_URL}/${id}`, kategoria);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
