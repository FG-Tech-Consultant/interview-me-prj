import React from 'react';
import { Helmet } from 'react-helmet-async';
import type { PublicProfileResponse } from '../../types/publicProfile';

interface PublicProfileSeoProps {
  profile: PublicProfileResponse;
}

export const PublicProfileSeo: React.FC<PublicProfileSeoProps> = ({ profile }) => {
  const { seo } = profile;

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Person',
    name: profile.fullName,
    jobTitle: profile.headline,
    url: seo.canonicalUrl,
    knowsAbout: profile.skills.map((s) => s.skillName),
    ...(profile.location && { address: { '@type': 'PostalAddress', addressLocality: profile.location } }),
  };

  return (
    <Helmet>
      <title>{seo.title}</title>
      <meta name="description" content={seo.description} />
      <link rel="canonical" href={seo.canonicalUrl} />

      {/* Open Graph */}
      <meta property="og:title" content={seo.title} />
      <meta property="og:description" content={seo.description} />
      <meta property="og:type" content="profile" />
      <meta property="og:url" content={seo.canonicalUrl} />

      {/* Twitter Card */}
      <meta name="twitter:card" content="summary" />
      <meta name="twitter:title" content={seo.title} />
      <meta name="twitter:description" content={seo.description} />

      {/* Keywords */}
      {seo.keywords.length > 0 && (
        <meta name="keywords" content={seo.keywords.join(', ')} />
      )}

      {/* JSON-LD Structured Data */}
      <script type="application/ld+json">{JSON.stringify(jsonLd)}</script>
    </Helmet>
  );
};
