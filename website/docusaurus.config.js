import {themes as prismThemes} from 'prism-react-renderer';

const repositoryName = process.env.GITHUB_REPOSITORY?.split('/')[1];
const isGitHubActions = process.env.GITHUB_ACTIONS === 'true';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'CS3227 MP1',
  tagline: 'Developer and user documentation',
  favicon: 'img/favicon.ico',
  url: process.env.DOCUSAURUS_URL ?? 'http://localhost',
  baseUrl: isGitHubActions && repositoryName ? `/${repositoryName}/` : '/',
  organizationName: process.env.GITHUB_REPOSITORY_OWNER ?? 'johnwz123',
  projectName: repositoryName ?? 'CS3227-2610-MP1',
  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  presets: [
    [
      'classic',
      {
        docs: {
          path: '..',
          include: ['README.md', 'docs/**/*.md'],
          exclude: ['docs/Reflections.md'],
          routeBasePath: '/',
          sidebarPath: './sidebars.js',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      },
    ],
  ],

  themeConfig: {
    navbar: {
      title: 'CS3227 MP1',
      items: [
        {type: 'docSidebar', sidebarId: 'guides', position: 'left', label: 'Guides'},
        {
          href: `https://github.com/${
            process.env.GITHUB_REPOSITORY ?? 'johnwz123/CS3227-2610-MP1'
          }`,
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Guides',
          items: [
            {label: 'Overview', to: '/'},
            {label: 'Developer Guide', to: '/DeveloperGuide'},
            {label: 'User Guide', to: '/UserGuide'},
          ],
        },
      ],
      copyright: `Copyright ${new Date().getFullYear()} CS3227 MP1.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  },
};

export default config;
