require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name           = package['name']
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = "MIT"
  s.author         = package['author']
  s.homepage       = "https://github.com/tanguyantoine/react-native-music-control"
  s.source         = { git: 'git://github.com/tanguyantoine/react-native-music-control.git', tag: s.version }

  s.requires_arc   = true
  s.platform       = :ios, '8.0'

  s.preserve_paths = 'LICENSE', 'README.md', 'package.json', 'index.js'
  s.source_files   = 'ios/*.{h,m}'

  s.dependency 'React-Core'
end
