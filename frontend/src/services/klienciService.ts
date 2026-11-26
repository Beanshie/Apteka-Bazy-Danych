import axios from 'axios';
import type { Klient, KlientCreate, KlientUpdate } from '../types/klient';

const API_URL = '/api/v1/klienci';

export const klienciService = {
    getAll: async (): Promise<Klient[]> => {
        const response = await axios.get(API_URL);
        return response.data;
    },

    getById: async (id: number): Promise<Klient> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (klient: KlientCreate): Promise<Klient> => {
        const response = await axios.post(API_URL, klient);
        return response.data;
    },

    update: async (id: number, klient: KlientUpdate): Promise<Klient> => {
        const response = await axios.put(`${API_URL}/${id}`, klient);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
