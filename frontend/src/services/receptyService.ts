import axios from 'axios';
import type {
    Recepta,
    ReceptaCreate,
    ReceptaUpdate,
    StatusRecepty,
} from '../types/recepta';

const API_URL = '/api/v1/recepty';

export const receptyService = {
    getAll: async (params?: {
        id_klienta?: number;
        kod_recepty?: string;
        status_recepty?: StatusRecepty;
    }): Promise<Recepta[]> => {
        const response = await axios.get(API_URL, { params });
        return response.data;
    },

    getById: async (id: number): Promise<Recepta> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (recepta: ReceptaCreate): Promise<Recepta> => {
        const response = await axios.post(API_URL, recepta);
        return response.data;
    },

    update: async (id: number, recepta: ReceptaUpdate): Promise<Recepta> => {
        const response = await axios.put(`${API_URL}/${id}`, recepta);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
