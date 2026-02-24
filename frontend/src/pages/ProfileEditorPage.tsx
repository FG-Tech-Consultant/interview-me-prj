import React, { useState } from 'react';
import { useCurrentProfile } from '../hooks/useProfile';
import { ProfileForm } from '../components/profile/ProfileForm';
import { JobExperienceList } from '../components/profile/JobExperienceList';
import { EducationList } from '../components/profile/EducationList';
import { SlugSettingsSection } from '../components/profile/SlugSettingsSection';

type TabType = 'profile' | 'experience' | 'education' | 'public';

export const ProfileEditorPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('profile');
  const { data: profile, isLoading, error } = useCurrentProfile();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Loading profile...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-red-600">
          Error loading profile: {error instanceof Error ? error.message : 'Unknown error'}
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <h1 className="text-3xl font-bold mb-6">Career Profile Editor</h1>

      {/* Tab Navigation */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('profile')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'profile'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Profile Information
          </button>
          <button
            onClick={() => setActiveTab('experience')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'experience'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Work Experience
          </button>
          <button
            onClick={() => setActiveTab('education')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'education'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Education
          </button>
          <button
            onClick={() => setActiveTab('public')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'public'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Public Profile
          </button>
        </nav>
      </div>

      {/* Tab Content */}
      <div className="bg-white rounded-lg shadow p-6">
        {activeTab === 'profile' && (
          <div>
            <h2 className="text-2xl font-semibold mb-4">Profile Information</h2>
            {profile ? (
              <ProfileForm profile={profile} />
            ) : (
              <p className="text-gray-600">No profile found. Please create one.</p>
            )}
          </div>
        )}

        {activeTab === 'experience' && (
          <div>
            <h2 className="text-2xl font-semibold mb-4">Work Experience</h2>
            {profile ? (
              <JobExperienceList profileId={profile.id} />
            ) : (
              <p className="text-gray-600">Create a profile first to add work experience.</p>
            )}
          </div>
        )}

        {activeTab === 'education' && (
          <div>
            <h2 className="text-2xl font-semibold mb-4">Education</h2>
            {profile ? (
              <EducationList profileId={profile.id} />
            ) : (
              <p className="text-gray-600">Create a profile first to add education.</p>
            )}
          </div>
        )}

        {activeTab === 'public' && (
          <div>
            <h2 className="text-2xl font-semibold mb-4">Public Profile Settings</h2>
            {profile ? (
              <SlugSettingsSection profileId={profile.id} currentSlug={profile.slug ?? null} />
            ) : (
              <p className="text-gray-600">Create a profile first to set up your public profile.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
