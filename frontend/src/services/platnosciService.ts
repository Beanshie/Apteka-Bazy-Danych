import axios from 'axios';
import type {
    Platnosc,
    PlatnoscCreate,
    PlatnoscUpdate,
} from '../types/platnosc';

const API_URL = '/api/v1/platnosci';

export const platnosciService = {
    getAll: async (params?: { id_zamowienia?: number }): Promise<Platnosc[]> => {
        const response = await axios.get(API_URL, { params });
        return response.data;
    },

    getById: async (id: number): Promise<Platnosc> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (platnosc: PlatnoscCreate): Promise<Platnosc> => {
        const response = await axios.post(API_URL, platnosc);
        return response.data;
    },

    update: async (id: number, platnosc: PlatnoscUpdate): Promise<Platnosc> => {
        const response = await axios.put(`${API_URL}/${id}`, platnosc);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
