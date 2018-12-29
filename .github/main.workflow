workflow "Build, Test, and Publish" {
  on = "push"
  resolves = [
    "Publish",
    "GitHub Action for npm",
  ]
}

action "Build" {
  uses = "actions/npm@master"
  args = "install"
}

action "Test" {
  needs = "Build"
  uses = "actions/npm@master"
  args = "test"
}

action "GitHub Action for npm" {
  uses = "actions/npm@e7aaefe"
  args = "install"
}//action "Publish" {
//  needs = "Test"
//  uses = "actions/npm@master"
//  args = "publish --access public"
//  secrets = ["NPM_AUTH_TOKEN"]
//}
