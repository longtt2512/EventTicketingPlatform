module.exports = config => config.set({
  frameworks: ['jasmine', '@angular-devkit/build-angular'],
  plugins: [require('karma-jasmine'), require('karma-chrome-launcher'), require('karma-coverage'), require('@angular-devkit/build-angular/plugins/karma')],
  reporters: ['progress', 'coverage'], browsers: ['ChromeHeadless'], singleRun: true,
  coverageReporter: { dir: require('path').join(__dirname, 'coverage/phase1'), subdir: '.', reporters: [{ type: 'html' }, { type: 'text-summary' }, { type: 'json-summary' }] },
});
