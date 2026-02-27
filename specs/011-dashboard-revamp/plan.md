# Implementation Plan: Dashboard Revamp & Navigation

## Architecture

### New Components
1. **AppLayout.tsx** - Main layout shell (sidebar + topbar + content area)
2. **Sidebar.tsx** - Navigation sidebar with route links
3. **TopBar.tsx** - App bar with user menu
4. **DashboardPage.tsx** - Complete rewrite of dashboard

### Dashboard Sub-Components
5. **ProfileCompletenessCard.tsx** - Progress ring + checklist
6. **QuickActionsGrid.tsx** - Action cards grid
7. **StatsCards.tsx** - Metric cards row
8. **RecentActivityCard.tsx** - Recent exports/analyses/chats
9. **PublicProfileCard.tsx** - Public URL display + copy

### Changes to Existing Files
10. **App.tsx** - Wrap protected routes with AppLayout
11. **Remove** standalone logout button from individual pages

## Implementation Strategy

- **No backend changes** - all data available from existing APIs
- **Frontend-only feature** - uses existing hooks
- Reuse existing hooks: useProfile, useSkills, useBilling, useExports, useLinkedInAnalysis
- New hook: **useDashboardData** - aggregates data from multiple hooks for dashboard

## Data Sources for Dashboard

| Widget | Hook/API | Data |
|--------|----------|------|
| Profile Completeness | useProfile, useSkills, useJobExperience, useEducation | Profile fields, counts |
| Stats Cards | useSkills, useBilling, useExports | Counts, balance |
| Quick Actions | Static | Navigation links |
| Recent Activity | useExports, useLinkedInAnalysis | Recent items |
| Public Profile | useProfile | Slug field |
