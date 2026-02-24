import React, { useState } from 'react';
import { useEducations, useDeleteEducation } from '../../hooks/useEducation';
import { EducationForm } from './EducationForm';
import type { Education } from '../../types/profile';

interface EducationListProps {
  profileId: number;
}

export const EducationList: React.FC<EducationListProps> = ({ profileId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data: educations, isLoading, error } = useEducations(profileId);
  const deleteMutation = useDeleteEducation();

  const handleDelete = (educationId: number) => {
    if (window.confirm('Are you sure you want to delete this education record?')) {
      deleteMutation.mutate({ profileId, educationId });
    }
  };

  const handleEdit = (educationId: number) => {
    setEditingId(educationId);
    setIsAdding(false);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
  };

  const handleAdd = () => {
    setIsAdding(true);
    setEditingId(null);
  };

  const handleCancelAdd = () => {
    setIsAdding(false);
  };

  if (isLoading) {
    return <div>Loading education records...</div>;
  }

  if (error) {
    return (
      <div className="text-red-600">
        Error loading education: {error instanceof Error ? error.message : 'Unknown error'}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Add Button */}
      {!isAdding && !editingId && (
        <button
          onClick={handleAdd}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          + Add Education
        </button>
      )}

      {/* Add Form */}
      {isAdding && (
        <div className="border border-gray-300 rounded-lg p-6 bg-gray-50">
          <h3 className="text-lg font-semibold mb-4">Add New Education</h3>
          <EducationForm
            profileId={profileId}
            onSuccess={handleCancelAdd}
            onCancel={handleCancelAdd}
          />
        </div>
      )}

      {/* Education List */}
      <div className="space-y-4">
        {educations && educations.length > 0 ? (
          educations.map((education) => (
            <div key={education.id} className="border border-gray-300 rounded-lg p-6">
              {editingId === education.id ? (
                <EducationForm
                  profileId={profileId}
                  education={education}
                  onSuccess={handleCancelEdit}
                  onCancel={handleCancelEdit}
                />
              ) : (
                <EducationCard
                  education={education}
                  onEdit={() => handleEdit(education.id)}
                  onDelete={() => handleDelete(education.id)}
                />
              )}
            </div>
          ))
        ) : (
          <p className="text-gray-600">No education records added yet. Click "Add Education" to get started.</p>
        )}
      </div>
    </div>
  );
};

interface EducationCardProps {
  education: Education;
  onEdit: () => void;
  onDelete: () => void;
}

const EducationCard: React.FC<EducationCardProps> = ({ education, onEdit, onDelete }) => {
  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
  };

  return (
    <div>
      <div className="flex justify-between items-start mb-3">
        <div>
          <h3 className="text-xl font-semibold">{education.degree}</h3>
          <p className="text-lg text-gray-700">{education.institution}</p>
          {education.fieldOfStudy && (
            <p className="text-gray-600">{education.fieldOfStudy}</p>
          )}
          <p className="text-sm text-gray-600">
            {education.startDate ? formatDate(education.startDate) : ''} - {education.endDate ? formatDate(education.endDate) : 'N/A'}
          </p>
        </div>
        <div className="flex space-x-2">
          <button
            onClick={onEdit}
            className="px-3 py-1 text-sm bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
          >
            Edit
          </button>
          <button
            onClick={onDelete}
            className="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700"
          >
            Delete
          </button>
        </div>
      </div>

      {education.gpa && (
        <p className="text-gray-700 mb-2">
          <span className="font-medium">GPA:</span> {education.gpa}
        </p>
      )}

      {education.notes && (
        <p className="text-gray-700 mb-3">{education.notes}</p>
      )}
    </div>
  );
};
