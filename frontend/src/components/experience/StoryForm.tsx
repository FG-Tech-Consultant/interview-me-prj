import React, { useState } from 'react';
import { useCreateStory, useUpdateStory } from '../../hooks/useStories';
import { MetricsEditor } from './MetricsEditor';
import type { StoryResponse, CreateStoryRequest, UpdateStoryRequest } from '../../types/story';

interface StoryFormProps {
  experienceProjectId: number;
  story?: StoryResponse;
  onSuccess: () => void;
  onCancel: () => void;
}

const STAR_HELPERS = {
  situation: 'Set the scene. What was the context?',
  task: 'What was your specific responsibility?',
  action: 'What steps did you take?',
  result: 'What was the outcome? Include metrics.',
};

export const StoryForm: React.FC<StoryFormProps> = ({
  experienceProjectId,
  story,
  onSuccess,
  onCancel,
}) => {
  const isEdit = !!story;
  const [title, setTitle] = useState(story?.title || '');
  const [situation, setSituation] = useState(story?.situation || '');
  const [task, setTask] = useState(story?.task || '');
  const [action, setAction] = useState(story?.action || '');
  const [result, setResult] = useState(story?.result || '');
  const [metrics, setMetrics] = useState<Record<string, unknown>>(
    (story?.metrics as Record<string, unknown>) || {}
  );
  const [visibility, setVisibility] = useState(story?.visibility || 'private');

  const createMutation = useCreateStory();
  const updateMutation = useUpdateStory();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (isEdit && story) {
      const data: UpdateStoryRequest = {
        title,
        situation,
        task,
        action,
        result,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        visibility,
        version: story.version,
      };
      updateMutation.mutate(
        { storyId: story.id, projectId: experienceProjectId, data },
        { onSuccess }
      );
    } else {
      const data: CreateStoryRequest = {
        title,
        situation,
        task,
        action,
        result,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        visibility,
      };
      createMutation.mutate({ projectId: experienceProjectId, data }, { onSuccess });
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700">Title *</label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          maxLength={255}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
          placeholder="Brief title for this STAR story"
        />
      </div>

      {(['situation', 'task', 'action', 'result'] as const).map((field) => {
        const value = { situation, task, action, result }[field];
        const setter = { situation: setSituation, task: setTask, action: setAction, result: setResult }[field];
        return (
          <div key={field}>
            <label className="block text-sm font-medium text-gray-700">
              {field.charAt(0).toUpperCase() + field.slice(1)} *
            </label>
            <textarea
              value={value}
              onChange={(e) => setter(e.target.value)}
              required
              maxLength={5000}
              rows={3}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder={STAR_HELPERS[field]}
            />
            <div className="text-xs text-gray-400 text-right mt-1">
              {value.length}/5000
            </div>
          </div>
        );
      })}

      <MetricsEditor value={metrics} onChange={setMetrics} />

      <div>
        <label className="block text-sm font-medium text-gray-700">Visibility</label>
        <select
          value={visibility}
          onChange={(e) => setVisibility(e.target.value)}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
        >
          <option value="private">Private</option>
          <option value="public">Public</option>
        </select>
      </div>

      <div className="flex space-x-3">
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
        >
          {isLoading ? 'Saving...' : isEdit ? 'Update Story' : 'Create Story'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
        >
          Cancel
        </button>
      </div>
    </form>
  );
};
