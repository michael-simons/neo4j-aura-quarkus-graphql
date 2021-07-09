module.exports = {
    // See https://cli.vuejs.org/config/#devserver-proxy
    devServer: {
        proxy: {
            '/graphql': {
                target: 'http://localhost:8080',
                ws: true,
                changeOrigin: true
            }
        }
    },
    outputDir: '../../../target/generated-resources/app',
    assetsDir: 'static'
};
