import React, { useState } from 'react';
import { useUpdateProfile, useCreateProfile } from '../../hooks/useProfile';
import type { Profile, CreateProfileRequest } from '../../types/profile';

interface ProfileFormProps {
  profile?: Profile;
}

export const ProfileForm: React.FC<ProfileFormProps> = ({ profile }) => {
  const [formData, setFormData] = useState({
    fullName: profile?.fullName || '',
    headline: profile?.headline || '',
    summary: profile?.summary || '',
    location: profile?.location || '',
  });

  const updateMutation = useUpdateProfile();
  const createMutation = useCreateProfile();

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (profile) {
      // Update existing profile
      updateMutation.mutate({
        profileId: profile.id,
        data: {
          fullName: formData.fullName,
          headline: formData.headline || undefined,
          summary: formData.summary || undefined,
          location: formData.location || undefined,
          version: profile.version,
        },
      });
    } else {
      // Create new profile
      const createData: CreateProfileRequest = {
        fullName: formData.fullName,
        headline: formData.headline || undefined,
        summary: formData.summary || undefined,
        location: formData.location || undefined,
      };
      createMutation.mutate(createData);
    }
  };

  const isLoading = updateMutation.isPending || createMutation.isPending;
  const error = updateMutation.error || createMutation.error;
  const isSuccess = updateMutation.isSuccess || createMutation.isSuccess;

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Basic Information */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label htmlFor="fullName" className="block text-sm font-medium text-gray-700 mb-1">
            Full Name *
          </label>
          <input
            type="text"
            id="fullName"
            name="fullName"
            value={formData.fullName}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="headline" className="block text-sm font-medium text-gray-700 mb-1">
            Headline
          </label>
          <input
            type="text"
            id="headline"
            name="headline"
            value={formData.headline}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      <div>
        <label htmlFor="summary" className="block text-sm font-medium text-gray-700 mb-1">
          Summary
        </label>
        <textarea
          id="summary"
          name="summary"
          value={formData.summary}
          onChange={handleChange}
          rows={4}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      {/* Location */}
      <div>
        <label htmlFor="location" className="block text-sm font-medium text-gray-700 mb-1">
          Location
        </label>
        <input
          type="text"
          id="location"
          name="location"
          value={formData.location}
          onChange={handleChange}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      {/* Success/Error Messages */}
      {isSuccess && (
        <div className="p-4 bg-green-50 border border-green-200 rounded-md">
          <p className="text-green-800">Profile saved successfully!</p>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-md">
          <p className="text-red-800">
            Error: {error instanceof Error ? error.message : 'Failed to save profile'}
          </p>
        </div>
      )}

      {/* Submit Button */}
      <div className="flex justify-end">
        <button
          type="submit"
          disabled={isLoading}
          className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {isLoading ? 'Saving...' : profile ? 'Update Profile' : 'Create Profile'}
        </button>
      </div>
    </form>
  );
};
