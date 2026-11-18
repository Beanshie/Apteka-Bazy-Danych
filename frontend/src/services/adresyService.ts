import axios from 'axios';
import type {
    AdresKlienta,
    AdresKlientaCreate,
    AdresKlientaUpdate,
} from '../types/adres_klienta';

const API_URL = '/api/v1/adresy';

export const adresyService = {
    getAll: async (params?: { id_klienta?: number }): Promise<AdresKlienta[]> => {
        const response = await axios.get(API_URL, { params });
        return response.data;
    },

    getById: async (id: number): Promise<AdresKlienta> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    create: async (adres: AdresKlientaCreate): Promise<AdresKlienta> => {
        const response = await axios.post(API_URL, adres);
        return response.data;
    },

    update: async (id: number, adres: AdresKlientaUpdate): Promise<AdresKlienta> => {
        const response = await axios.put(`${API_URL}/${id}`, adres);
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/${id}`);
    },
};
