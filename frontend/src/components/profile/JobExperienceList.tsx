import React, { useState } from 'react';
import { useJobExperiences, useDeleteJobExperience } from '../../hooks/useJobExperience';
import { JobExperienceForm } from './JobExperienceForm';
import { ProjectList } from '../experience/ProjectList';
import type { JobExperience } from '../../types/profile';

interface JobExperienceListProps {
  profileId: number;
}

export const JobExperienceList: React.FC<JobExperienceListProps> = ({ profileId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data: experiences, isLoading, error } = useJobExperiences(profileId);
  const deleteMutation = useDeleteJobExperience();

  const handleDelete = (experienceId: number) => {
    if (window.confirm('Are you sure you want to delete this job experience?')) {
      deleteMutation.mutate({ profileId, experienceId });
    }
  };

  const handleEdit = (experienceId: number) => {
    setEditingId(experienceId);
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
    return <div>Loading job experiences...</div>;
  }

  if (error) {
    return (
      <div className="text-red-600">
        Error loading job experiences: {error instanceof Error ? error.message : 'Unknown error'}
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
          + Add Job Experience
        </button>
      )}

      {/* Add Form */}
      {isAdding && (
        <div className="border border-gray-300 rounded-lg p-6 bg-gray-50">
          <h3 className="text-lg font-semibold mb-4">Add New Job Experience</h3>
          <JobExperienceForm
            profileId={profileId}
            onSuccess={handleCancelAdd}
            onCancel={handleCancelAdd}
          />
        </div>
      )}

      {/* Experience List */}
      <div className="space-y-4">
        {experiences && experiences.length > 0 ? (
          experiences.map((experience) => (
            <div key={experience.id} className="border border-gray-300 rounded-lg p-6">
              {editingId === experience.id ? (
                <JobExperienceForm
                  profileId={profileId}
                  experience={experience}
                  onSuccess={handleCancelEdit}
                  onCancel={handleCancelEdit}
                />
              ) : (
                <JobExperienceCard
                  experience={experience}
                  onEdit={() => handleEdit(experience.id)}
                  onDelete={() => handleDelete(experience.id)}
                />
              )}
            </div>
          ))
        ) : (
          <p className="text-gray-600">No job experiences added yet. Click "Add Job Experience" to get started.</p>
        )}
      </div>
    </div>
  );
};

interface JobExperienceCardProps {
  experience: JobExperience;
  onEdit: () => void;
  onDelete: () => void;
}

const JobExperienceCard: React.FC<JobExperienceCardProps> = ({ experience, onEdit, onDelete }) => {
  const [showProjects, setShowProjects] = useState(false);

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
  };

  return (
    <div>
      <div className="flex justify-between items-start mb-3">
        <div>
          <h3 className="text-xl font-semibold">{experience.role}</h3>
          <p className="text-lg text-gray-700">{experience.company}</p>
          <p className="text-sm text-gray-600">
            {formatDate(experience.startDate)} - {experience.isCurrent ? 'Present' : experience.endDate ? formatDate(experience.endDate) : 'N/A'}
            {experience.location && ` \u2022 ${experience.location}`}
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

      {experience.responsibilities && (
        <p className="text-gray-700 mb-3">{experience.responsibilities}</p>
      )}

      {experience.achievements && (
        <div className="mb-3">
          <h4 className="font-medium text-gray-900 mb-1">Achievements:</h4>
          <p className="text-gray-700">{experience.achievements}</p>
        </div>
      )}

      {/* Projects Section */}
      <div className="mt-3 pt-3 border-t border-gray-200">
        <button
          onClick={() => setShowProjects(!showProjects)}
          className="text-sm font-medium text-blue-600 hover:text-blue-800"
        >
          {showProjects ? 'Hide Projects' : 'Show Projects & Stories'}
        </button>
        {showProjects && <ProjectList jobExperienceId={experience.id} />}
      </div>
    </div>
  );
};
