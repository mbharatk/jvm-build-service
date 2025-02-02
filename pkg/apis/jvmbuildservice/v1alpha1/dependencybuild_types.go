package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

const (
	DependencyBuildStateNew          = "DependencyBuildStateNew"
	DependencyBuildStateAnalyzeBuild = "DependencyBuildStateAnalyzeBuild"
	DependencyBuildStateSubmitBuild  = "DependencyBuildStateSubmitBuild"
	DependencyBuildStateBuilding     = "DependencyBuildStateBuilding"
	DependencyBuildStateComplete     = "DependencyBuildStateComplete"
	DependencyBuildStateFailed       = "DependencyBuildStateFailed"
	DependencyBuildStateContaminated = "DependencyBuildStateContaminated"
)

type DependencyBuildSpec struct {
	ScmInfo SCMInfo `json:"scm,omitempty"`
	Version string  `json:"version,omitempty"`
}

type DependencyBuildStatus struct {
	// Conditions for capturing generic status
	// NOTE: inspecting the fabric8 Status class, it looked analogous to k8s Condition,
	// and then I took the liberty of making it an array, given best practices in the k8s/ocp ecosystems
	Conditions   []metav1.Condition `json:"conditions,omitempty"`
	State        string             `json:"state,omitempty"`
	Message      string             `json:"message,omitempty"`
	Contaminants []string           `json:"contaminates,omitempty"`
	//BuildRecipe the current build recipe. If build is done then this recipe was used
	//to get to the current state
	CurrentBuildRecipe *BuildRecipe `json:"currentBuildRecipe,omitempty"`
	//PotentialBuildRecipes additional recipes to try if the current recipe fails
	PotentialBuildRecipes []*BuildRecipe `json:"potentialBuildRecipes,omitempty"`
	//FailedBuildRecipes recipes that resulted in a failure
	//if the current state is failed this may include the current BuildRecipe
	FailedBuildRecipes        []*BuildRecipe `json:"failedBuildRecipes,omitempty"`
	LastCompletedBuildTaskRun string         `json:"lastCompletedBuildTaskRun,omitempty"`
}

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:subresource:status
// +kubebuilder:resource:path=dependencybuilds,scope=Namespaced
// +kubebuilder:printcolumn:name="URL",type=string,JSONPath=`.spec.scm.scmURL`
// +kubebuilder:printcolumn:name="Tag",type=string,JSONPath=`.spec.scm.tag`
// +kubebuilder:printcolumn:name="State",type=string,JSONPath=`.status.state`
// +kubebuilder:printcolumn:name="Message",type=string,JSONPath=`.status.message`

// DependencyBuild TODO provide godoc description
type DependencyBuild struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DependencyBuildSpec   `json:"spec"`
	Status DependencyBuildStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// DependencyBuildList contains a list of DependencyBuild
type DependencyBuildList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DependencyBuild `json:"items"`
}

type BuildRecipe struct {
	Task             string   `json:"task,omitempty"`
	Image            string   `json:"image,omitempty"`
	CommandLine      []string `json:"commandLine,omitempty"`
	EnforceVersion   string   `json:"enforceVersion,omitempty"`
	IgnoredArtifacts []string `json:"ignoredArtifacts,omitempty"`
}
