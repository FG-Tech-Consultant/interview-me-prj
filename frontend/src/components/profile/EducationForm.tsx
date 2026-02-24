import React, { useState } from 'react';
import { useCreateEducation, useUpdateEducation } from '../../hooks/useEducation';
import type { Education, CreateEducationRequest } from '../../types/profile';

interface EducationFormProps {
  profileId: number;
  education?: Education;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export const EducationForm: React.FC<EducationFormProps> = ({
  profileId,
  education,
  onSuccess,
  onCancel,
}) => {
  const [formData, setFormData] = useState({
    institution: education?.institution || '',
    degree: education?.degree || '',
    fieldOfStudy: education?.fieldOfStudy || '',
    startDate: education?.startDate || '',
    endDate: education?.endDate || '',
    gpa: education?.gpa || '',
    notes: education?.notes || '',
  });

  const createMutation = useCreateEducation();
  const updateMutation = useUpdateEducation();

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

    if (education) {
      // Update existing
      updateMutation.mutate(
        {
          profileId,
          educationId: education.id,
          data: {
            institution: formData.institution,
            degree: formData.degree,
            fieldOfStudy: formData.fieldOfStudy || undefined,
            startDate: formData.startDate || undefined,
            endDate: formData.endDate,
            gpa: formData.gpa || undefined,
            notes: formData.notes || undefined,
            version: education.version,
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
      const createData: CreateEducationRequest = {
        institution: formData.institution,
        degree: formData.degree,
        fieldOfStudy: formData.fieldOfStudy || undefined,
        startDate: formData.startDate || undefined,
        endDate: formData.endDate,
        gpa: formData.gpa || undefined,
        notes: formData.notes || undefined,
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
          <label htmlFor="institution" className="block text-sm font-medium text-gray-700 mb-1">
            Institution *
          </label>
          <input
            type="text"
            id="institution"
            name="institution"
            value={formData.institution}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="degree" className="block text-sm font-medium text-gray-700 mb-1">
            Degree *
          </label>
          <input
            type="text"
            id="degree"
            name="degree"
            value={formData.degree}
            onChange={handleChange}
            required
            placeholder="e.g., Bachelor of Science"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      <div>
        <label htmlFor="fieldOfStudy" className="block text-sm font-medium text-gray-700 mb-1">
          Field of Study
        </label>
        <input
          type="text"
          id="fieldOfStudy"
          name="fieldOfStudy"
          value={formData.fieldOfStudy}
          onChange={handleChange}
          placeholder="e.g., Computer Science"
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
            Start Date
          </label>
          <input
            type="date"
            id="startDate"
            name="startDate"
            value={formData.startDate}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">
            End Date *
          </label>
          <input
            type="date"
            id="endDate"
            name="endDate"
            value={formData.endDate}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      <div>
        <label htmlFor="gpa" className="block text-sm font-medium text-gray-700 mb-1">
          GPA
        </label>
        <input
          type="text"
          id="gpa"
          name="gpa"
          value={formData.gpa}
          onChange={handleChange}
          placeholder="e.g., 3.8/4.0, First Class Honours"
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      <div>
        <label htmlFor="notes" className="block text-sm font-medium text-gray-700 mb-1">
          Notes
        </label>
        <textarea
          id="notes"
          name="notes"
          value={formData.notes}
          onChange={handleChange}
          rows={3}
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
          {isLoading ? 'Saving...' : education ? 'Update' : 'Add'}
        </button>
      </div>
    </form>
  );
};
