import React, { useState } from 'react';
import { useCreateJobExperience, useUpdateJobExperience } from '../../hooks/useJobExperience';
import type { JobExperience, CreateJobExperienceRequest } from '../../types/profile';

interface JobExperienceFormProps {
  profileId: number;
  experience?: JobExperience;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export const JobExperienceForm: React.FC<JobExperienceFormProps> = ({
  profileId,
  experience,
  onSuccess,
  onCancel,
}) => {
  const [formData, setFormData] = useState({
    company: experience?.company || '',
    role: experience?.role || '',
    location: experience?.location || '',
    startDate: experience?.startDate || '',
    endDate: experience?.endDate || '',
    isCurrent: experience?.isCurrent || false,
    employmentType: experience?.employmentType || '',
    responsibilities: experience?.responsibilities || '',
    achievements: experience?.achievements || '',
  });

  const createMutation = useCreateJobExperience();
  const updateMutation = useUpdateJobExperience();

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (experience) {
      // Update existing
      updateMutation.mutate(
        {
          profileId,
          experienceId: experience.id,
          data: {
            company: formData.company,
            role: formData.role,
            location: formData.location || undefined,
            startDate: formData.startDate,
            endDate: formData.isCurrent ? undefined : formData.endDate || undefined,
            isCurrent: formData.isCurrent,
            employmentType: formData.employmentType || undefined,
            responsibilities: formData.responsibilities || undefined,
            achievements: formData.achievements || undefined,
            version: experience.version,
          },
        },
        {
          onSuccess: () => {
            onSuccess?.();
          },
        }
      );
    } else {
      // Create new
      const createData: CreateJobExperienceRequest = {
        company: formData.company,
        role: formData.role,
        location: formData.location || undefined,
        startDate: formData.startDate,
        endDate: formData.isCurrent ? undefined : formData.endDate || undefined,
        isCurrent: formData.isCurrent,
        employmentType: formData.employmentType || undefined,
        responsibilities: formData.responsibilities || undefined,
        achievements: formData.achievements || undefined,
      };

      createMutation.mutate(
        { profileId, data: createData },
        {
          onSuccess: () => {
            onSuccess?.();
          },
        }
      );
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;
  const error = createMutation.error || updateMutation.error;

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label htmlFor="role" className="block text-sm font-medium text-gray-700 mb-1">
            Role *
          </label>
          <input
            type="text"
            id="role"
            name="role"
            value={formData.role}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="company" className="block text-sm font-medium text-gray-700 mb-1">
            Company *
          </label>
          <input
            type="text"
            id="company"
            name="company"
            value={formData.company}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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

        <div>
          <label htmlFor="employmentType" className="block text-sm font-medium text-gray-700 mb-1">
            Employment Type
          </label>
          <input
            type="text"
            id="employmentType"
            name="employmentType"
            value={formData.employmentType}
            onChange={handleChange}
            placeholder="e.g., Full-time, Part-time, Contract"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
            Start Date *
          </label>
          <input
            type="date"
            id="startDate"
            name="startDate"
            value={formData.startDate}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">
            End Date {!formData.isCurrent && '*'}
          </label>
          <input
            type="date"
            id="endDate"
            name="endDate"
            value={formData.endDate}
            onChange={handleChange}
            disabled={formData.isCurrent}
            required={!formData.isCurrent}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
          />
        </div>
      </div>

      <div className="flex items-center">
        <input
          type="checkbox"
          id="isCurrent"
          name="isCurrent"
          checked={formData.isCurrent}
          onChange={handleChange}
          className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
        />
        <label htmlFor="isCurrent" className="ml-2 block text-sm text-gray-700">
          I currently work here
        </label>
      </div>

      <div>
        <label htmlFor="responsibilities" className="block text-sm font-medium text-gray-700 mb-1">
          Responsibilities
        </label>
        <textarea
          id="responsibilities"
          name="responsibilities"
          value={formData.responsibilities}
          onChange={handleChange}
          rows={3}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      <div>
        <label htmlFor="achievements" className="block text-sm font-medium text-gray-700 mb-1">
          Achievements
        </label>
        <textarea
          id="achievements"
          name="achievements"
          value={formData.achievements}
          onChange={handleChange}
          rows={3}
          placeholder="Key achievements in this role"
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-md">
          <p className="text-red-800 text-sm">
            Error: {error instanceof Error ? error.message : 'Failed to save'}
          </p>
        </div>
      )}

      <div className="flex justify-end space-x-3">
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50"
          >
            Cancel
          </button>
        )}
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {isLoading ? 'Saving...' : experience ? 'Update' : 'Add'}
        </button>
      </div>
    </form>
  );
};
