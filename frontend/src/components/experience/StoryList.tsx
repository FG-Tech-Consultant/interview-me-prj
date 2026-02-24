import React, { useState } from 'react';
import { useStories, useDeleteStory } from '../../hooks/useStories';
import { StoryForm } from './StoryForm';
import type { StoryResponse } from '../../types/story';

interface StoryListProps {
  experienceProjectId: number;
}

export const StoryList: React.FC<StoryListProps> = ({ experienceProjectId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const { data: stories, isLoading, error } = useStories(experienceProjectId);
  const deleteMutation = useDeleteStory();

  const handleDelete = (storyId: number) => {
    if (window.confirm('Are you sure you want to delete this story?')) {
      deleteMutation.mutate({ storyId, projectId: experienceProjectId });
    }
  };

  if (isLoading) return <div className="text-sm text-gray-500">Loading stories...</div>;
  if (error) return <div className="text-sm text-red-600">Error loading stories.</div>;

  return (
    <div className="space-y-2">
      {!isAdding && !editingId && (
        <button
          onClick={() => setIsAdding(true)}
          className="px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          + Add Story
        </button>
      )}

      {isAdding && (
        <div className="border border-gray-200 rounded p-3 bg-gray-50">
          <h5 className="text-sm font-semibold mb-2">Add STAR Story</h5>
          <StoryForm
            experienceProjectId={experienceProjectId}
            onSuccess={() => setIsAdding(false)}
            onCancel={() => setIsAdding(false)}
          />
        </div>
      )}

      {stories && stories.length > 0 ? (
        stories.map((story) => (
          <div key={story.id} className="border border-gray-200 rounded p-3">
            {editingId === story.id ? (
              <StoryForm
                experienceProjectId={experienceProjectId}
                story={story}
                onSuccess={() => setEditingId(null)}
                onCancel={() => setEditingId(null)}
              />
            ) : (
              <StoryCard
                story={story}
                isExpanded={expandedId === story.id}
                onToggleExpand={() =>
                  setExpandedId(expandedId === story.id ? null : story.id)
                }
                onEdit={() => {
                  setEditingId(story.id);
                  setIsAdding(false);
                }}
                onDelete={() => handleDelete(story.id)}
              />
            )}
          </div>
        ))
      ) : (
        !isAdding && (
          <p className="text-xs text-gray-500">
            No stories yet. Add a STAR story to prepare for behavioral interviews.
          </p>
        )
      )}
    </div>
  );
};

interface StoryCardProps {
  story: StoryResponse;
  isExpanded: boolean;
  onToggleExpand: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

const truncate = (text: string, maxLen: number) =>
  text.length > maxLen ? text.slice(0, maxLen) + '...' : text;

const StoryCard: React.FC<StoryCardProps> = ({
  story,
  isExpanded,
  onToggleExpand,
  onEdit,
  onDelete,
}) => {
  return (
    <div>
      <div className="flex justify-between items-start">
        <div className="flex-1 cursor-pointer" onClick={onToggleExpand}>
          <h5 className="text-sm font-semibold">{story.title}</h5>
          <div className="flex gap-2 mt-1">
            <span className={`text-xs px-1.5 py-0.5 rounded ${
              story.visibility === 'public'
                ? 'bg-green-100 text-green-700'
                : 'bg-gray-100 text-gray-600'
            }`}>
              {story.visibility}
            </span>
          </div>
          {!isExpanded && (
            <p className="text-xs text-gray-500 mt-1">
              S: {truncate(story.situation, 80)}
            </p>
          )}
        </div>
        <div className="flex space-x-1 ml-2">
          <button
            onClick={onEdit}
            className="px-2 py-0.5 text-xs bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
          >
            Edit
          </button>
          <button
            onClick={onDelete}
            className="px-2 py-0.5 text-xs bg-red-600 text-white rounded hover:bg-red-700"
          >
            Delete
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="mt-2 pt-2 border-t border-gray-100 space-y-2">
          {(['Situation', 'Task', 'Action', 'Result'] as const).map((label) => {
            const key = label.toLowerCase() as keyof Pick<StoryResponse, 'situation' | 'task' | 'action' | 'result'>;
            return (
              <div key={label}>
                <span className="text-xs font-semibold text-gray-600">{label}:</span>
                <p className="text-sm text-gray-700 whitespace-pre-wrap">{story[key]}</p>
              </div>
            );
          })}

          {story.metrics && Object.keys(story.metrics).length > 0 && (
            <div>
              <span className="text-xs font-semibold text-gray-600">Metrics:</span>
              <div className="flex flex-wrap gap-1 mt-1">
                {Object.entries(story.metrics).map(([key, val]) => (
                  <span key={key} className="text-xs bg-purple-50 text-purple-700 px-2 py-0.5 rounded">
                    {key}: {String(val)}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
