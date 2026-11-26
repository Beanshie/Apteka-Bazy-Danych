import axios from 'axios';
import type {
    Dostawca,
    DostawcaCreate,
    DostawcaUpdate,
} from '../types/dostawca';

const API_URL = '/api/v1/dostawcy';

export const dostawcyService = {
    getAll: async (): Promise<Dostawca[]> => {
        const response = await axios.get(API_URL);
        return response.data;
    },

    getById: async (id: number): Promise<Dostawca> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (dostawca: DostawcaCreate): Promise<Dostawca> => {
        const response = await axios.post(API_URL, dostawca);
        return response.data;
    },

    update: async (id: number, dostawca: DostawcaUpdate): Promise<Dostawca> => {
        const response = await axios.put(`${API_URL}/${id}`, dostawca);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
