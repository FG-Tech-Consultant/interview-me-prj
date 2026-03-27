import axios from 'axios';
import apiClient from './client';
import type { PublicProfileResponse, SlugCheckResponse } from '../types/publicProfile';
import type { Profile } from '../types/profile';

// Separate axios instance for public endpoints (no auth token)
const publicClient = axios.create({
  baseURL: `${import.meta.env.BASE_URL}api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const publicProfileApi = {
  // Get public profile by slug (NO auth required)
  getPublicProfile: async (slug: string): Promise<PublicProfileResponse> => {
    const response = await publicClient.get<PublicProfileResponse>(
      `/public/profiles/${slug}`
    );
    return response.data;
  },

  // Check slug availability (requires auth)
  checkSlugAvailability: async (slug: string): Promise<SlugCheckResponse> => {
    const response = await apiClient.get<SlugCheckResponse>(
      `/profiles/slug/check`,
      { params: { slug } }
    );
    return response.data;
  },

  // Update profile slug (requires auth)
  updateSlug: async (profileId: number, slug: string): Promise<Profile> => {
    const response = await apiClient.put<Profile>(
      `/profiles/${profileId}/slug`,
      { slug }
    );
    return response.data;
  },
};
