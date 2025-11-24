// Імпорт необхідних модулів
const path = require('path');
const { VueLoaderPlugin } = require('vue-loader');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { DefinePlugin } = require('webpack');
const Dotenv = require('dotenv-webpack');
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')

module.exports = {
  // Встановлення режиму збірки (production або development)
  mode: process.env.NODE_ENV === 'production' ? 'production' : 'development',
  // Вхідна точка додатку
  entry: './src/main.js',
  // Налаштування виводу збірки
  output: {
    filename: '[name].[contenthash].js',
    path: path.resolve(__dirname, 'dist'),
    clean: true,
    publicPath: '/',
    crossOriginLoading: false,
    globalObject: 'self'
  },
  // Налаштування модулів та правил обробки файлів
  module: {
    rules: [
      // Обробка Vue файлів
      {
        test: /\.vue$/,
        loader: 'vue-loader',
      },
      // Обробка JavaScript файлів
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env'],
            plugins: ['@babel/plugin-transform-runtime'],
          },
        },
      },
      // Обробка SCSS файлів
      {
        test: /\.scss$/,
        use: [
          process.env.NODE_ENV === 'production'
            ? MiniCssExtractPlugin.loader
            : 'style-loader',
          {
            loader: 'css-loader',
            options: {
              sourceMap: true,
            },
          },
          {
            loader: 'postcss-loader',
            options: {
              postcssOptions: {
                plugins: ['autoprefixer'],
              },
            },
          },
          'sass-loader',
        ],
      },
      // Обробка CSS файлів
      {
        test: /\.css$/,
        use: [
          process.env.NODE_ENV === 'production'
            ? MiniCssExtractPlugin.loader
            : 'style-loader',
          {
            loader: 'css-loader',
            options: {
              sourceMap: true,
            },
          },
        ],
      },
      // Обробка зображень
      {
        test: /\.(png|webp|jpe?g|gif|svg)$/,
        type: 'asset',
        parser: {
          dataUrlCondition: {
            maxSize: 8 * 1024, // 8kb
          },
        },
        generator: {
          filename: 'images/[name].[hash][ext]',
        },
      },
      // Обробка шрифтів
      {
        test: /\.(woff2?|eot|ttf|otf)$/,
        type: 'asset',
        generator: {
          filename: 'fonts/[name].[hash][ext]',
        },
      },
    ],
  },
  // Налаштування резолвера
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      'vue': '@vue/runtime-dom',
    },
    // Резервні модулі для браузера
    fallback: {
      "url": require.resolve("url/"),
      "https": require.resolve("https-browserify"),
      "http": require.resolve("stream-http"),
      "util": require.resolve("util/"),
      "buffer": require.resolve("buffer/"),
      "stream": require.resolve("stream-browserify"),
    },
    extensions: ['.js', '.vue', '.json'],
  },
  // Налаштування source maps
  devtool: process.env.NODE_ENV === 'production' ? 'source-map' : 'eval-cheap-module-source-map',
  // Налаштування сервера розробки
  devServer: {
    static: {
      directory: path.join(__dirname, 'dist'),
    },
    compress: true,
    port: 9000,
    hot: true,
    historyApiFallback: true,
    allowedHosts: 'all',
    client: {
      overlay: true,
    },
    headers: {
      'Cross-Origin-Embedder-Policy': 'credentialless',
      'Cross-Origin-Opener-Policy': 'same-origin-allow-popups',
    },
    setupMiddlewares: (middlewares, devServer) => {
      // Middleware для обработки worker файлов
      devServer.app.get('/*.worker.js', (req, res, next) => {
        res.set({
          'Content-Type': 'application/javascript; charset=utf-8',
          'Cache-Control': 'no-cache',
        });
        next();
      });
      
      return middlewares;
    },
  },
  // Налаштування оптимізації
  optimization: {
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        monacoEditor: {
          test: /[\\/]node_modules[\\/]monaco-editor[\\/]/,
          name: 'monaco-editor',
          chunks: 'all',
          priority: 10,
          enforce: true,
        },
      },
    },
  },
  // Налаштування плагінів
  plugins: [
    new VueLoaderPlugin(),
    new HtmlWebpackPlugin({
      template: './dist/index.html',
      favicon: './dist/favicon.ico',
      title: 'Vue App',
    }),
    new MiniCssExtractPlugin({
      filename: 'css/[name].[contenthash].css',
    }),
    new DefinePlugin({
      __VUE_OPTIONS_API__: true,
      __VUE_PROD_DEVTOOLS__: false,
    }),
    new Dotenv({
      path: '.env',
      systemvars: true,
    }),
    new MonacoWebpackPlugin({
      // Указываем какие языки нужны
      languages: [
        'javascript',
        'cpp', 'csharp', 'java', 'python', 'rust', 'go', 'php',
        'ruby', 'swift', 'kotlin', 'dart', 'pascal', 'perl', 'lua',
        'haskell', 'plaintext'
      ],
      features: [
        'bracketMatching', 'clipboard', 'codeAction', 'coreCommands', 
        'find', 'folding', 'format', 'hover', 'linesOperations', 
        'multicursor', 'suggest', 'wordHighlighter', 'wordOperations',
        'gotoLine', 'indentation'
      ],
      publicPath: '/',
      globalAPI: false,
    })
  ],
};