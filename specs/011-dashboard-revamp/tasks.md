# Tasks: Dashboard Revamp & Navigation

## Task 1: Create AppLayout with Sidebar and TopBar
- Create `frontend/src/components/layout/AppLayout.tsx`
- Create `frontend/src/components/layout/Sidebar.tsx`
- Create `frontend/src/components/layout/TopBar.tsx`
- MUI Drawer sidebar with navigation items
- Collapsible sidebar (toggle icon-only mode)
- TopBar with app name and user menu (email + logout)
- Active route highlighting
- Responsive: permanent drawer on desktop, temporary on mobile

## Task 2: Integrate AppLayout into App.tsx
- Wrap all ProtectedRoute content with AppLayout
- Remove standalone logout buttons from pages that have them
- Ensure routing works within the layout

## Task 3: Build Dashboard - Stats Cards
- Create `frontend/src/components/dashboard/StatsCards.tsx`
- Cards: Skills count, Job experiences, Stories, Coin balance, Exports, Chat sessions
- Use existing hooks to fetch data
- MUI Card grid layout

## Task 4: Build Dashboard - Profile Completeness Card
- Create `frontend/src/components/dashboard/ProfileCompletenessCard.tsx`
- Circular progress showing completion %
- Checklist: profile filled, has jobs, has education, has skills, has stories, slug set
- Each item links to relevant page

## Task 5: Build Dashboard - Quick Actions Grid
- Create `frontend/src/components/dashboard/QuickActionsGrid.tsx`
- Action cards: Edit Profile, Add Skill, Export Resume, Analyze LinkedIn, View Public Profile
- Each with MUI icon, title, short description
- onClick navigates to the page

## Task 6: Build Dashboard - Recent Activity & Public Profile
- Create `frontend/src/components/dashboard/RecentActivityCard.tsx`
- Create `frontend/src/components/dashboard/PublicProfileCard.tsx`
- Recent exports with status badges
- Recent LinkedIn analyses with scores
- Public profile URL with copy-to-clipboard

## Task 7: Rewrite DashboardPage
- Compose all dashboard components into the page
- Use grid layout: stats top, completeness + quick actions middle, activity + public profile bottom
- Loading states for each section independently
- Welcome message with user name

## Task 8: Final Integration & Polish
- Test all navigation links work
- Verify responsive behavior
- Ensure no regressions on other pages
- Clean up any dead code (old logout buttons, etc.)
