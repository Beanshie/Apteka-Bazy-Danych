import axios from 'axios';
import type { Produkt, ProduktCreate, ProduktUpdate } from '../types/produkt';

const API_URL = '/api/v1/produkty';

export const produktyService = {
    getAll: async (): Promise<Produkt[]> => {
        const response = await axios.get(API_URL);
        return response.data;
    },

    create: async (produkt: ProduktCreate): Promise<Produkt> => {
        const response = await axios.post(API_URL, produkt);
        return response.data;
    },

    update: async (id: number, produkt: ProduktUpdate): Promise<Produkt> => {
        const response = await axios.put(`${API_URL}/${id}`, produkt);
        return response.data;
    },
    getById: async (id: number): Promise<Produkt> => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },
};