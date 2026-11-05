import axios from 'axios';
import type { Lek, DaneLek } from '../types/lek';

const API_URL = '/api/v1/leki';

export const lekiService = {
    getAll: async (): Promise<Lek[]> => {
        const response = await axios.get(API_URL);
        return response.data;
    },

    create: async (lek: DaneLek): Promise<Lek> => {
        const response = await axios.post(API_URL, lek);
        return response.data;
    },

    update: async (id: number, lek: DaneLek): Promise<Lek> => {
        const response = await axios.put(`${API_URL}/${id}`, lek);
        return response.data;
    },
    getById: async (id: number): Promise<Lek> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },
};