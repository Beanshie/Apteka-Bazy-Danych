import axios from 'axios';
import type {
    Pracownik,
    PracownikCreate,
    PracownikUpdate,
} from '../types/pracownik';

const API_URL = '/api/v1/pracownicy';

export const pracownicyService = {
    getAll: async (): Promise<Pracownik[]> => {
        const response = await axios.get(API_URL);
        return response.data;
    },

    getById: async (id: number): Promise<Pracownik> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (pracownik: PracownikCreate): Promise<Pracownik> => {
        const response = await axios.post(API_URL, pracownik);
        return response.data;
    },

    update: async (id: number, pracownik: PracownikUpdate): Promise<Pracownik> => {
        const response = await axios.put(`${API_URL}/${id}`, pracownik);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
