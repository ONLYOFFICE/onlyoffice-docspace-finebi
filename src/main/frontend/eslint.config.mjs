import preact from 'eslint-config-preact';
import tseslint from 'typescript-eslint';

const preactConfigs = preact.map((config) => ({
  ...config,
  files: ['**/*.{js,jsx,mjs,cjs,ts,tsx}'],
  languageOptions: {
    ...config.languageOptions,
    parser: tseslint.parser,
    parserOptions: {
      ...config.languageOptions?.parserOptions,
      ecmaFeatures: { jsx: true },
    },
  },
  rules: {
    ...config.rules,
    'no-unused-vars': 'off',
    'no-undef': 'off',
    'no-duplicate-imports': 'off',
    'prefer-template': 'off',
    quotes: ['error', 'double', { avoidEscape: true, allowTemplateLiterals: true }],
    'max-len': ['error', { code: 120, ignoreUrls: true, ignoreStrings: true, ignoreTemplateLiterals: true }],
  },
}));

export default [
  {
    ignores: ['dist/**', 'node_modules/**'],
  },
  ...preactConfigs,
];
